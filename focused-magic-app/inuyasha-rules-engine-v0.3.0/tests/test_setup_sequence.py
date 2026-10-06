import unittest
from inuyasha_engine import GameState, PlayerState, CharacterState, AttachmentState, Color, RuleError, TimingWindow, SetupCard, CardKind

class SetupSequenceTests(unittest.TestCase):
    def game(self):
        g=GameState({'A':PlayerState('A',draw_pile=[f'a{i}' for i in range(20)]), 'B':PlayerState('B',draw_pile=[f'b{i}' for i in range(20)])},'A')
        draw=g.start_turn_and_draw();g.start_setup(draw)
        return g
    def card(self,g,id,kind=CardKind.CHARACTER,player='A',**kw):
        defaults=dict(title=id,character_name=id if kind==CardKind.CHARACTER else '',colors={Color.BLUE:3})
        defaults.update(kw);c=SetupCard(id,kind=kind,**defaults);g.setup_cards[id]=c;g.player(player).hand.append(id);return c
    def test_order_and_boundary(self):
        g=self.game();self.assertEqual([(e.window,e.subject_player_id) for e in g.event_log[-2:]],[(TimingWindow.SETUP_START,'A'),(TimingWindow.SETUP_START,'B')])
        with self.assertRaises(RuleError):g.finish_setup_actions('B')
        self.card(g,'c');self.card(g,'l',CardKind.LOCATION);self.card(g,'i',CardKind.ITEM)
        g.play_setup_card('A','l');g.play_setup_card('A','c');g.play_setup_card('A','i',host_id='c')
        g.finish_setup_actions('A');self.card(g,'b',player='B');g.play_setup_card('B','b');g.finish_setup_actions('B')
        self.assertEqual(g.setup_context.phase,'BATTLE_START');self.assertEqual(g.turn_active_player_id,'A')
        self.assertEqual([(e.window,e.subject_player_id) for e in g.event_log[-2:]],[(TimingWindow.SETUP_END,'A'),(TimingWindow.SETUP_END,'B')])
        with self.assertRaises(RuleError):g.play_setup_card('A','c')
    def test_start_end_hooks_each_player_order(self):
        g=GameState({'A':PlayerState('A',draw_pile=list('abcdef')),'B':PlayerState('B',draw_pile=list('ghijkl'))},'A');seen=[]
        for w in [TimingWindow.SETUP_START,TimingWindow.SETUP_END]:g.register_window_handler(w,lambda game,ctx,event:seen.append((event.window,event.subject_player_id,ctx.acting_player_id)))
        g.start_setup(g.start_turn_and_draw());g.finish_setup_actions('A');g.finish_setup_actions('B')
        self.assertEqual([x[1:] for x in seen],[('A','A'),('B','B'),('A','A'),('B','B')])
    def test_two_characters_per_player_and_atomic_rejection(self):
        g=self.game()
        for player in ['A','B']:
            for i in range(3):self.card(g,player+str(i),player=player,deck_cost=1)
            for i in range(2):g.play_setup_card(player,player+str(i))
            before=(list(g.player(player).draw_pile),list(g.player(player).hand))
            with self.assertRaises(RuleError):g.play_setup_card(player,player+'2')
            self.assertEqual(before,(g.player(player).draw_pile,g.player(player).hand))
            g.finish_setup_actions(player)
    def test_items_locations_repeat_and_full_cost_paid_first(self):
        g=self.game();events=[]
        for id,kind in [('c',CardKind.CHARACTER),('l',CardKind.LOCATION),('l2',CardKind.LOCATION),('i',CardKind.ITEM)]:
            self.card(g,id,kind,deck_cost=1,when_played=lambda game,p,id:events.append((id,list(game.player(p).discard_pile))))
        for id in ['l','c','l2']:g.play_setup_card('A',id)
        g.play_setup_card('A','i',host_id='c');self.assertEqual(len(events),4);self.assertEqual([len(e[1]) for e in events],[1,2,3,4]);self.assertEqual(g.setup_context.characters_played['A'],1)
    def test_reject_duplicate_location_and_second_item(self):
        g=self.game();self.card(g,'c');g.play_setup_card('A','c')
        for id in ['l','copy']:self.card(g,id,CardKind.LOCATION,title='Village')
        g.play_setup_card('A','l')
        with self.assertRaises(RuleError):g.play_setup_card('A','copy')
        for id in ['i','j']:self.card(g,id,CardKind.ITEM)
        g.play_setup_card('A','i',host_id='c')
        with self.assertRaises(RuleError):g.play_setup_card('A','j',host_id='c')
    def test_overlay_counts_as_second_character_and_discards_attachments(self):
        g=self.game();self.card(g,'old',title='Kagome, Student',character_name='Kagome',surname='Student',unique=True);self.card(g,'new',title='Kagome, Archer',character_name='Kagome',surname='Archer',unique=True);self.card(g,'i',CardKind.ITEM)
        g.play_setup_card('A','old');g.play_setup_card('A','i',host_id='old');g.play_setup_card('A','new');self.assertEqual(g.setup_context.characters_played['A'],2);self.assertEqual(g.player('A').discard_pile,['old','i']);self.assertFalse(g.characters[0].in_play)
    def test_same_surname_overlay_rejected(self):
        g=self.game()
        for id in ['c','d']:self.card(g,id,title='Kagome, Student',character_name='Kagome',surname='Student',unique=True)
        g.play_setup_card('A','c')
        with self.assertRaises(RuleError):g.play_setup_card('A','d')
    def test_insufficient_cost_and_wrong_actor_do_not_mutate(self):
        g=self.game();self.card(g,'c',deck_cost=99);before=list(g.player('A').hand)
        with self.assertRaises(RuleError):g.play_setup_card('A','c')
        with self.assertRaises(RuleError):g.play_setup_card('B','c')
        self.assertEqual(g.player('A').hand,before)
    def test_tiara_steals_and_counts_character_limit_and_restores_owner(self):
        g=self.game();self.card(g,'cho',character_name='Chokyukai');g.play_setup_card('A','cho');self.card(g,'victim',player='B');g.characters.append(CharacterState('victim','B',{Color.RED:2},card_id='victim',owner_id='B'));g.player('B').hand.remove('victim')
        self.card(g,'tiara',CardKind.ITEM,title="Chokyukai's Tiara");g.play_setup_card('A','tiara',host_id='victim');self.assertEqual(g.characters[-1].controller_id,'A');self.assertEqual(g.setup_context.characters_played['A'],2)
        g.remove_setup_attachment('victim','tiara');self.assertEqual(g.characters[-1].controller_id,'B')
    def test_tiara_requires_chokyukai_and_opposing_target(self):
        g=self.game();self.card(g,'c');g.play_setup_card('A','c');self.card(g,'tiara',CardKind.ITEM,title="Chokyukai's Tiara")
        with self.assertRaises(RuleError):g.play_setup_card('A','tiara',host_id='c')
    def test_scent_return_search_shuffle_reveal_and_once_limit(self):
        g=self.game();self.card(g,'c');g.play_setup_card('A','c');self.card(g,'s',CardKind.EVENT,title='Following the Scent');self.card(g,'s2',CardKind.EVENT,title='Following the Scent')
        self.card(g,'loc',CardKind.LOCATION);g.player('A').hand.remove('loc');g.player('A').draw_pile.append('loc');shuffled=[]
        g.play_setup_card('A','s',return_character_id='c',search_ids=['loc'],shuffle=lambda pool:shuffled.append(list(pool)))
        self.assertIn('c',g.player('A').hand);self.assertIn('loc',g.player('A').hand);self.assertTrue(shuffled);self.assertEqual(g.setup_context.revealed_searches,[('A',['loc'])]);self.assertEqual(g.setup_context.cut_offers,[('A','B')]);g.play_setup_card('A','c')
        with self.assertRaises(RuleError):g.play_setup_card('A','s2',return_character_id='c')
    def test_scent_invalid_selection_no_cost_paid(self):
        g=self.game();self.card(g,'c');g.play_setup_card('A','c');self.card(g,'s',CardKind.EVENT,title='Following the Scent',deck_cost=1);before=list(g.player('A').draw_pile)
        with self.assertRaises(RuleError):g.play_setup_card('A','s',return_character_id='c',search_ids=['not-location'])
        self.assertEqual(before,g.player('A').draw_pile)
    def test_shroom_allows_shippo_copies_and_stays_active_defeated(self):
        g=self.game();self.card(g,'s1',title='Shippo, Artist',character_name='Shippo',surname='Artist',unique=True);g.play_setup_card('A','s1');self.card(g,'shroom',CardKind.EVENT,title="Shippo's Shroom");g.play_setup_card('A','shroom',host_id='s1');self.card(g,'s2',title='Shippo, Artist',character_name='Shippo',surname='Artist',unique=True);g.play_setup_card('A','s2');self.assertEqual(len(g.characters),2);self.assertTrue(g.characters[0].in_play);g.characters[0].defeat();self.assertFalse(g._is_unique_character(g.setup_cards['s2'],'A'))
        with self.assertRaises(RuleError):g.remove_setup_attachment('s1','shroom',by_effect=True)
    def test_item_technique_moves_item_without_replaying_it(self):
        g=self.game();self.card(g,'c');self.card(g,'d');g.play_setup_card('A','c');g.play_setup_card('A','d');self.card(g,'item',CardKind.ITEM);g.play_setup_card('A','item',host_id='c');self.card(g,'event',CardKind.EVENT,title="Shippo's Item Technique");before=len(g.event_log);g.play_setup_card('A','event',host_id='d',item_id='item');self.assertEqual(g.characters[0].attachments,[]);self.assertEqual(g.characters[1].attachments[0].card_id,'item');self.assertEqual(len(g.event_log),before)
    def test_unverified_setup_event_blocked(self):
        g=self.game();self.card(g,'event',CardKind.EVENT)
        with self.assertRaises(RuleError):g.play_setup_card('A','event')
    def test_duplicate_setup_and_incomplete_draw_blocked(self):
        g=self.game();draw=g.start_turn_and_draw();draw.draw_step_complete=False
        with self.assertRaises(RuleError):g.start_setup(draw)
        draw.draw_step_complete=True;g.start_setup(draw)
        with self.assertRaises(RuleError):g.start_setup(draw)

    def test_card_restrictions_and_attachment_restrictions_fail_before_cost(self):
        g=self.game();self.card(g,'c');g.play_setup_card('A','c');self.card(g,'item',CardKind.ITEM,deck_cost=1,can_attach=lambda game,host,player:False)
        before=list(g.player('A').draw_pile)
        with self.assertRaises(RuleError):g.play_setup_card('A','item',host_id='c')
        self.assertEqual(g.player('A').draw_pile,before)
        self.card(g,'loc',CardKind.LOCATION,can_play=lambda game,player:False)
        with self.assertRaises(RuleError):g.play_setup_card('A','loc')
    def test_card_played_window_order_matches_ard_procedures(self):
        g=self.game();seen=[]
        for w in [TimingWindow.CHARACTER_PLAYED,TimingWindow.ITEM_PLAYED,TimingWindow.LOCATION_PLAYED]:g.register_window_handler(w,lambda game,ctx,event:seen.append('window:'+event.detail))
        for id,kind in [('c',CardKind.CHARACTER),('i',CardKind.ITEM),('l',CardKind.LOCATION)]:self.card(g,id,kind,when_played=lambda game,player,id:seen.append('effect:'+id))
        g.play_setup_card('A','c');g.play_setup_card('A','i',host_id='c');g.play_setup_card('A','l')
        self.assertEqual(seen,['effect:c','window:c','window:i','effect:i','window:l','effect:l'])
    def test_scent_can_search_discard_and_choose_zero(self):
        g=self.game();self.card(g,'c');g.play_setup_card('A','c');self.card(g,'s',CardKind.EVENT,title='Following the Scent');self.card(g,'loc',CardKind.LOCATION);g.player('A').hand.remove('loc');g.player('A').discard_pile.append('loc')
        g.play_setup_card('A','s',return_character_id='c',search_zone='discard',search_ids=['loc']);self.assertIn('loc',g.player('A').hand);self.assertEqual(g.setup_context.cut_offers,[])
        h=self.game();self.card(h,'c');h.play_setup_card('A','c');self.card(h,'s',CardKind.EVENT,title='Following the Scent');h.play_setup_card('A','s',return_character_id='c',search_ids=[]);self.assertIn('c',h.player('A').hand)

if __name__ == '__main__':unittest.main()
