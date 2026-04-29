package com.project.habithearth.ui.story

// Scripted Chapter 1 graph. Each section opens with a long prose page that ends
// in a decision; choices route to short flavor pages, then a shared interlude,
// then converge on the next section's intro. Section 6 ends in a cliffhanger
// followed by a "To be continued" page that recaps the player's dominant pick.
//
// Background reuse: only four hand-painted backgrounds exist for six sections,
// so sections 3 and 6 reuse adjacent art. No protagonist sprite ships with this
// chapter — the player is rendered as second-person prose only.
//
// Gating: each intro node carries `requiredLevel`; the view model blocks
// advancement until the player levels up. Categorized choices carry a gem cost
// to encourage habit completions in that category before they unlock.

private const val BG_DESTROYED = "images/backgrounds/destroyedVillage.png"
private const val BG_BUILDING = "images/backgrounds/buildingVillage1.png"
private const val BG_BUILT = "images/backgrounds/builtVillage.png"
private const val BG_HOT_SPRINGS = "images/backgrounds/hot_springs.png"

private const val CH_GARRICK = "images/characters/Garrick.PNG"
private const val CH_THISTLE = "images/characters/Thistle.PNG"
private const val CH_SABLE = "images/characters/Sable.PNG"
private const val CH_CLOVER = "images/characters/Clover.PNG"
private const val CH_ASHFORD = "images/characters/Ashford.PNG"
private const val CH_CHESTER = "images/characters/Chester.PNG"

// Default cost for any categorized choice. Cheap enough that two completed
// habits in that category unlock it; tunable per-choice if a section needs
// to feel heavier.
private const val DEFAULT_GEM_COST = 3

data class StoryChoice(
    val label: String,
    val category: String? = null,
    val gemCost: Int = 0,
    val nextNodeId: String,
)

data class StoryNode(
    val id: String,
    val text: String,
    val backgroundAsset: String? = null,
    val characterAssets: List<String> = emptyList(),
    val choices: List<StoryChoice> = emptyList(),
    val nextNodeId: String? = null,
    // Minimum player level required to enter this node. Only enforced on intro
    // nodes today, but applied uniformly so a future interlude can also gate.
    val requiredLevel: Int = 1,
    val isEnding: Boolean = false,
)

object Chapter1 {

    const val START_ID = "s1_intro"
    const val TITLE = "Chapter 1: Survival"

    // Category keys must match the four gem categories tracked on GameUiState.
    // Used both for gem-cost gating in the screen and for tallying the
    // dominant pick at the chapter epilogue.
    const val CATEGORY_STRENGTH = "Strength"
    const val CATEGORY_WISDOM = "Wisdom"
    const val CATEGORY_VITALITY = "Vitality"
    const val CATEGORY_SPIRIT = "Spirit"

    private val sectionOnePrompt = """
        The dragons really did a number on this place.

        You stand in the middle of the village square, looking around at what's left. Four buildings, all in various states of "that's not great." The mayor's house has no roof. The library has half a wall. The training grounds look like someone picked them up and set them back down crooked. The hot springs pavilion is mostly fine, except for the part where the hot springs aren't working.

        Chester is already at your side. You haven't known him long, but you've learned two things: he always has his clipboard, and he always has a plan. "So here's the thing," he says, tapping his pencil against his front teeth. "We've got about a week before the rain makes all of this worse. But I've got a plan. Several plans, actually." He flips a page. "Want to hear plan C first? It's my favorite."

        Before you can answer, something catches your eye at the edge of the square. Purple vines. They're crawling out from the treeline, slow and lazy, curling around the nearest pile of rubble. A light fog follows behind them, purple-tinted and smelling faintly of old basements.

        Garrick steps out of the training grounds, arms crossed. His cape is singed at the edges but he doesn't seem to have noticed. "That's the Veil," he says. "Shows up when people stop working. It's not dangerous. It's just persistent. Like a weed that judges you." He looks you over. "You the engineer?" You nod. "Good. Pick a building and start. The vines get bolder when nobody's doing anything."
    """.trimIndent()

    private val sectionTwoPrompt = """
        Rubble, it turns out, is heavier than it looks. And there is so much of it.

        Chester follows you around with his clipboard, sketching measurements and muttering about load-bearing walls. He's surprisingly fast for someone shaped like a log with legs. Garrick has organized a hauling crew from the village guard. There are four of them, and one has a sprained wrist, but they work without complaining.

        By midday, you've cleared enough debris to see the floor of the first building. It's cracked but solid. Chester kneels and knocks on it. "We can work with this," he says, grinning so wide you can count his teeth.

        Then you hear a voice from behind a collapsed wall. Small, precise, and deeply irritated. "Excuse me. If someone could remove this bookshelf from my desk, I would appreciate it. It has been sitting on my research for three days and I have been too polite to shout about it until now."

        You pull the shelf aside. Underneath it, Thistle is sitting cross-legged on the floor, surrounded by singed papers. "Ah," he says, adjusting his cracked glasses. "You must be the engineer. Excellent. I have compiled a list of structural priorities." He holds up a scroll longer than he is tall. "It is alphabetized. You're welcome. Also, the south wall of the library is going to fall down within two days. I did not put it on the list because it is urgent, not important. Those are different categories."
    """.trimIndent()

    private val sectionThreePrompt = """
        By the third day, the vines have gotten bold.

        They're not fast. That's what makes them annoying. You'll set down a beam, turn around to grab another one, and when you look back there's a vine draped across it like it lives there. One of them wrapped itself around Chester's clipboard while he was writing. He didn't notice until his pencil wouldn't move.

        Thistle has set up a research station in the least-damaged corner of the library. He's been studying a piece of vine under a magnifying glass. "It reacts to inactivity," he tells you. "When people are busy, it retreats. When they stop, it creeps forward. It's like a plant that feeds on procrastination. Academically interesting. Practically inconvenient."

        Garrick's approach is less scientific. When a vine gets too close, he pulls it off whatever it's clinging to and tosses it. When the fog drifts toward his crew, he makes them do jumping jacks until it backs off. "Fog doesn't like exercise," he says. That might not be true, but the fog does leave.

        On the fourth morning, you find a thick cluster of vines wrapped around the front door of the mayor's house. They're thicker than the others and very comfortable-looking, like they've decided this is their door now.
    """.trimIndent()

    private val sectionFourPrompt = """
        Five days of hauling rocks will do things to your back that you didn't think were possible.

        Then you hear splashing. Behind the hot springs pavilion, someone has cleared the drainage channel. Warm water is flowing again for the first time since the attack, pooling in a shallow basin. Steam curls off the surface, and where it drifts toward the nearest vine, the vine wilts like a noodle.

        Clover is standing ankle-deep in the water, waving at you with both hands. "Hi! I fixed the pipes!" she calls out. "Well, most of them. Two of them are still definitely broken. But the important ones work!" She climbs out, shaking water off her tail. "Oh, you look exhausted. When did you last sit down? Have you eaten today? You should eat."

        She's already pulling a jar out of her satchel. "Healing salve. I made it from the herbs growing near the springs. Fire ash is apparently great for lavender. Who knew?"

        Garrick is standing near the pool, watching the steam curl toward a vine. The vine droops and sags. "The springs could be useful," he says, "if we can aim the steam." Thistle appears with his notebook already open. "If channeled through the original pipe network, the thermal output could cover roughly sixty percent of the vine perimeter."
    """.trimIndent()

    private val sectionFivePrompt = """
        Day six. The vines are backing off.

        Not everywhere, and not all at once. But the edges of the square are clearer than yesterday. The fog has pulled back past the treeline. The four buildings are still rough — walls patched with planks, tarps, one section that appears to be held together with rope and optimism — but everything is standing. People are going inside. Work is happening.

        Chester walks the square with you that evening, clipboard tucked under his arm for once. "When I first saw this place, I thought we'd need a year. Maybe two." He looks at the patched roofs. "Turns out people move fast when the alternative is purple weeds eating their stuff."

        Garrick finds you near the training grounds at sunset. The sky is orange and warm. "Not done yet. The vines will try again. They always do. And the buildings need real repairs, not patches. But today was a good day. You're allowed to have those."

        Thistle walks over from the library, carrying a single book. "I found something in the records. The original village had a foundation system. Anchors set deep into the bedrock. They were damaged in the attack, but the design is right here." He holds up the book like it's made of gold. "We will need this for what comes next."
    """.trimIndent()

    private val sectionSixPrompt = """
        You wake up to a sound you haven't heard before. A low, grinding creak coming from somewhere underneath the floor.

        Chester is already outside when you step into the square. He's kneeling with his palm flat on the ground. His usual grin is missing. "The foundation," he says. "It's shifting."

        You look down. Thin cracks are spreading across the stone floor of the square, branching out like lines on a leaf. They're not big. But they weren't there yesterday.

        Garrick shows up a moment later. He doesn't kneel. He just stands very still and feels it through his boots. "The dragon fire. It cooked the ground underneath us. Turned the dirt into something like glass. And glass doesn't hold forever."

        Thistle confirms it within the hour, scribbling calculations on every flat surface he can find. "The heat hardened the soil into a brittle layer about two meters deep. It was stable when the buildings above were destroyed. But now that we're rebuilding, now that we're adding weight back — well. It's cracking."

        Garrick uncrosses his arms. He almost never does that. "So. What's the plan, engineer?"

        Clover arrives last, satchel over her shoulder. "Whatever we do, we figure it out together. That's how it works around here." The vines at the treeline have stopped retreating. They're not advancing either. They're just sitting there, like they're waiting to see what happens next.
    """.trimIndent()

    // Decision-point flavor responses. Each is the immediate "you do X, here's
    // what happened" beat that bridges a chosen action into the section's
    // shared interlude.

    private val s1FlavorDefault = """
        You grab the nearest broken beam and start hauling. Chester scribbles furiously, his pencil tapping out a rhythm against his teeth as he revises the plan in real time. Garrick gives a short, approving grunt and gets back to lifting things he probably shouldn't lift alone — a beam, a section of wall, what may have once been a door frame.

        By the time the sun is high, the square has shifted from "unrecognizable" to "ambitious project." Chester glances up from his clipboard, looks at the cleared patch of stone, and grins. "Plan C it is, then. Brilliant."
    """.trimIndent()

    private val s1FlavorStrength = """
        You head straight for the training grounds. The collapsed beam is heavier than it looks, but so are you when you've decided to be. You set your feet, plant your shoulder, and drive upward — once, twice, three times — until the thing finally shifts.

        Garrick watches the whole sequence without moving, then nods once and joins you on the second pass. Together you clear the doorway in a single afternoon. "Good," he says, brushing dust off his cape. "The grounds are mine. Let's clear them."
    """.trimIndent()

    private val s1FlavorWisdom = """
        You step into the library first. Most of the blueprints are ash, but a tin tube under the desk survived — sealed at both ends, scorched but intact. You pry it open and unroll a length of vellum that shows the original village layout in full detail: load-bearing walls, drainage, even where the springs used to feed each building.

        Thistle is going to have feelings about this. He arrives within the hour, glasses askew, hands already reaching. "Oh — blueprints. You cannot imagine my relief."
    """.trimIndent()

    private val s1FlavorVitality = """
        You clear the path to the hot springs. Steam is already trying to escape from a cracked pipe, and the trail of warmth has cleared a narrow strip of vines on its own. Clover is, somehow, already there, fussing over a wilted herb she's named Greg.

        She holds a sprig of lavender up to the steam and watches the leaves perk back to attention like they remembered an appointment. She turns to you, beaming. "Greg lives! Greg lives!"
    """.trimIndent()

    private val s1FlavorSpirit = """
        You whistle once, loud, and the scattered villagers drift toward the square. Nobody calls a meeting. They just start moving in the same direction, which is most of what a meeting is anyway.

        Within ten minutes there are ten people sorting tools, three more clearing rubble, and a young fox carrying water without anyone telling him to. Chester is delighted. He marks something down, draws a small star next to it, and circles the star. "Look at that. People showing up. Don't tell me planning doesn't work."
    """.trimIndent()

    private val s2FlavorDefault = """
        You tell Chester to add it to the schedule. He writes 'SOUTH WALL (SCARY)' in three different colors and then keeps moving. The wall is already creaking — slowly, politely, like furniture in a humid room — but the schedule has it queued behind two other tasks Chester insists are equally important.

        By the next morning the wall is queued first. By midday it is braced. "I'm marking it red," Chester says, holding up a freshly recolored note. "Red means scary."
    """.trimIndent()

    private val s2FlavorStrength = """
        Garrick hauls four timbers across the square in two trips. The wall complains. The timbers don't. He sets each one with the slow, considered confidence of someone who has lifted heavier and lived.

        When he steps back, the south wall has stopped sagging entirely — and looks, somehow, faintly embarrassed about how close it came. "The wall stays up," he says, dusting his paws. "That is the deal we made."
    """.trimIndent()

    private val s2FlavorWisdom = """
        Thistle works the math out loud while you watch. He sketches the load on the back of a recipe scroll, mutters about lateral force, then circles two specific points on the diagram. You end up bracing the wall with two timbers instead of six.

        The library's south wall stops sagging within the hour, and Thistle, briefly satisfied, allows himself the smallest of smiles. "Two timbers," he says. "Mathematically sufficient. Aesthetically perfect."
    """.trimIndent()

    private val s2FlavorVitality = """
        You move the villagers out from under the wall first. Three children are very upset to leave their fort. One demands to know your authority. Another offers to fight you for it.

        The wall comes down forty minutes later, exactly where they were sitting — a thunderclap of dust and timber that quiets the whole square for a full minute. Clover finds the children first, hands them snacks, and then turns to you, her voice light but iron-firm. "Nobody under a wall on my watch. That is a rule now."
    """.trimIndent()

    private val s2FlavorSpirit = """
        You turn the wall job into a teaching session. Six villagers learn how to set a brace. One of them is better at it than you are. By dusk you have not just a stabilized wall but a small crew that can stabilize the next one without you.

        Ashford the secretary bird, who has been observing politely from a stack of crates, dips his beak in approval. "An admirable demonstration," he says. "The technique should be taught more broadly."
    """.trimIndent()

    private val s3FlavorDefault = """
        You yank vines off the door with both hands. The door hates this. You hate this. The vines, eventually, lose — but only after a solid hour of arguing in pantomime, with you pulling and them rooting deeper just to spite you.

        Chester arrives near the end, takes one look at the scene, and adds a fresh column to his clipboard labeled simply VINES (PERSONAL). "I now hate vines on a personal level," he says, underlining each word.
    """.trimIndent()

    private val s3FlavorStrength = """
        You take Garrick's heavy shears to the thickest stems. The cluster comes off in three big sections, each one heavier than the last, the final piece dragging a small avalanche of loose mortar with it.

        The door, miraculously, is fine. Garrick looks proud — actually, openly proud — for almost a full second before pretending he wasn't looking at all. "Three pieces," he says, already turning to the next building. "Door's clean. Next."
    """.trimIndent()

    private val s3FlavorWisdom = """
        Thistle mixes a solvent with three things you didn't know we had. He stirs slowly, narrating each ingredient like a court witness, then upends the whole flask onto the cluster.

        The vines wilt in long ribbons, slide off the door without a scratch, and pool gently at his feet — which he steps over with the dignity of someone who anticipated everything. "Behold," he announces to no one. "Applied chemistry. Take notes if you must."
    """.trimIndent()

    private val s3FlavorVitality = """
        You run a steam line from the springs in buckets. The vines retreat the moment the heat touches them, like cats hearing a vacuum. By the third bucket the cluster has lost its grip entirely; by the fifth, the whole doorframe is clear and faintly steaming.

        Clover arrives with a sixth bucket anyway, just to be thorough. "Hot water beats vines," she says, dumping it over the threshold. "Big news, I know."
    """.trimIndent()

    private val s3FlavorSpirit = """
        You count to three. Twelve villagers pull on twelve vines. The cluster comes off the door all at once with a sound like a giant exhaling, followed by a small cheer from everyone present except Garrick, who refuses to cheer in public.

        Chester is delighted. He writes '12-VINE PULL' in his clipboard and underlines it. "Twelve people, twelve vines," he says, proudly. "Math even I can do."
    """.trimIndent()

    private val s4FlavorDefault = """
        You let Clover keep running the springs. Within the hour she's organized a soaking schedule, a tea rotation, and at least one nap. By midafternoon she has a small clipboard of her own (Chester is very flattered), with names and times and herbal notes in a handwriting nobody else can read.

        Villagers wander in tired and shuffle out distinctly less tired. "Schedule's posted. Tea's brewing. Backs are healing," Clover says, when you wave her over. "Welcome."
    """.trimIndent()

    private val s4FlavorStrength = """
        Garrick's crew digs trenches all afternoon. By sundown, three of the worst vine clusters are wilting in long, sad lines, each one steaming gently in the cooling air. The crew is filthy, exhausted, and unmistakably pleased with itself.

        Garrick walks the trenches at dusk like a man inspecting his own front yard. "Trenches dug. Steam routed. Vines complaining," he says, stopping next to you. "Acceptable day."
    """.trimIndent()

    private val s4FlavorWisdom = """
        You and Thistle spend the day under the springs with old pipe diagrams. He maps the original network on a slate, finds a buried branch nobody had used in a decade, and spends two hours arguing with himself about whether to cap or restore it. He restores it.

        By morning, steam reaches places it hasn't reached in a decade — including a tile patch that promptly softens enough to scrub. "I have updated the pipe schematic," Thistle says, holding up the slate. "It is, frankly, art."
    """.trimIndent()

    private val s4FlavorVitality = """
        Clover brews a small pot, then a larger one, then realizes she needs the largest pot. The largest pot is a salvaged cauldron from the old kitchen, which she scrubs personally before using.

        Everyone who drinks it stops complaining about their backs, and several start humming. She hands you a cup with the seriousness of someone passing on tradition. "Drink up," she says. "Don't ask what's in it. Just trust the otter."
    """.trimIndent()

    private val s4FlavorSpirit = """
        You set up benches, a kettle, and a bowl for boots. Within an hour, the springs become the place people want to be — and the place where work, somehow, gets faster.

        Sable, who arrived in the last hour with her telescope strapped to her back, kneels beside the pool and runs her fingers through the water like she's checking that it remembers her. She looks up at you. "A communal hearth," she says. "Long overdue. The stars approve."
    """.trimIndent()

    private val s5FlavorDefault = """
        You sit down. The fire is warm. Your back stops hurting eventually. You don't think about the vines for a full forty minutes — possibly longer, but Chester eventually appears with a bowl of something Clover made, and time gets fuzzy after that.

        The square is quiet in the way only working squares are quiet: full of small sounds, none of them alarming. Chester settles next to you, finally still. "Sit. Eat. We earned it," he says, then squints at the firelight. "Probably."
    """.trimIndent()

    private val s5FlavorStrength = """
        You walk the perimeter with Garrick. He doesn't say much. Neither do you. The vines stay where they are, which is the whole point — and the moonlight catches the patched roofs in a way that makes the village look briefly like an illustration of itself.

        Garrick stops once, at the treeline, to nudge a vine with his boot. The vine, sensibly, doesn't move. "Quiet perimeter," he says. "Quiet bear. Good night."
    """.trimIndent()

    private val s5FlavorWisdom = """
        You and Thistle go through the foundation diagrams by lamplight. He marks anchor points in red ink, draws connecting lines between them, and rechecks every measurement twice because that is who he is.

        You don't sleep, but you don't mind — there's a particular pleasure to watching a problem shrink in real time. He sets the pen down near dawn, exhausted and pleased. "These anchor points will hold," he says. "I am ninety-three percent certain."
    """.trimIndent()

    private val s5FlavorVitality = """
        You help Clover bottle salves until your hands smell like lavender for two days straight. The work is repetitive in the best way: scoop, seal, label, set aside.

        By the end, the shelves are lined with small clay jars that look like a museum exhibit of better days. The villagers will be glad in the morning. Clover stretches, cracking her knuckles. "Salves bottled. Bandages rolled," she says. "Bring me chaos tomorrow."
    """.trimIndent()

    private val s5FlavorSpirit = """
        You sit with the villagers around the fire. Someone tells the dragon story. Someone else tells it better. By the end, no one is afraid of tomorrow — they're tired, yes, and hungry, and aching in places they didn't know existed, but the fear has gone.

        Ashford, perched on a crate at the edge of the firelight, watches the gathering with his usual long-necked dignity. "Tonight we rest," he says, when the laughter quiets. "Tomorrow, we begin again, properly."
    """.trimIndent()

    // Interludes — quieter scenes that sit between sections. Single shared
    // node per section, intentionally short so the per-choice flavor remains
    // the primary "you did this" reward.

    private val interlude1 = """
        That night, the village square is quieter than it was in the morning. A handful of villagers are still awake, sorting tools by lamplight. Someone's got a kettle going on a small fire.

        Chester sits on a flipped crate, finally still for the first time in two days. He's chewing his pencil. "First day's the hardest," he says, not really to you. "Tomorrow's the second hardest. After that it's just days."

        Garrick walks the perimeter once before turning in. He doesn't say goodnight. He just nods at the treeline like he's daring it to do something. The vines, sensibly, don't.

        You can hear the fog rolling in past the buildings. It sounds like nothing, which is somehow worse than sound.
    """.trimIndent()

    private val interlude2 = """
        Thistle has claimed a corner of the rebuilt library and refuses to leave it. He has stacks. He has piles. He has, at one point, two pencils tucked behind one ear.

        "I have catalogued thirty-seven distinct survival concerns," he announces to nobody. "I have ranked them. I have color-coded the rankings. I am not currently sleeping."

        Clover wanders in halfway through the day with a cup of something steaming. She sets it on his desk without a word and leaves. He drinks half of it absentmindedly while annotating a load chart and audibly says "oh."

        By evening the south wall is upright, the first roof is sealed, and three more villagers have shown up out of the woods asking what they can carry.
    """.trimIndent()

    private val interlude3 = """
        The vines retreat from the mayor's door, but only a few feet. They are not embarrassed. They are not deterred. They are, if anything, taking notes.

        Chester adds a new column to his clipboard labeled simply "PLANT BEHAVIOR." Under it, he writes: "rude."

        In the late afternoon, a young villager shows up at the square dragging a wooden cart. Inside the cart are tools you didn't know the village still owned — saws, levels, a brass plumb bob older than anyone alive. "Found them in my grandfather's shed," the villager says. "He told me to bring them when the dragons came back. He said you would need them more than we would."

        You find a quiet moment to set the plumb bob down on Chester's desk. He stares at it for a long time without speaking. Then he writes, in very careful letters: "this changes things."
    """.trimIndent()

    private val interlude4 = """
        Word travels fast in a village this small. By dusk, half the survivors have made the walk to the hot springs at least once. Some of them stay.

        Sable arrives that evening. You haven't seen her in months — not since she went up the mountain to chart the stars before the attack. Her telescope is strapped to her back. She kneels beside the steaming water without ceremony, dips her fingers in, and exhales like she's been holding her breath for a week. "It's still here," she says. "Good." She doesn't elaborate.

        Clover, who has been everywhere at once all day, finally sits down on a rock near the pool. She looks at the steam, the wilting vines, the villagers laughing about something stupid in the shallow end. "This place is going to make it," she says. "I didn't know that yesterday."
    """.trimIndent()

    private val interlude5 = """
        You sleep more soundly than you have in a week. So does everyone else.

        Around three in the morning, a brief rain falls — the first since the attack. It's gentle, almost polite. Chester is up at first light to inspect the patched roofs. Three are dry. One is leaking, but only in the specific corner Chester predicted it would leak. He looks insufferably pleased.

        The vines at the treeline don't move during the rain. They don't move when the sun rises. They don't move when the first villagers begin walking the square again.

        You start to wonder if the Veil is only the first problem. Whether something else, quieter, has been waiting for you all along.
    """.trimIndent()

    // To-be-continued + diary recap. Diary text is selected at runtime in the
    // view model based on the player's most-picked category, so it shows up as
    // a small "the story remembers what you did" reward at chapter close.

    val diaryDefault = "Notes from the engineer's journal: 'Got through the week. Did a little of everything. The village is still standing. I am, mostly, also.'"
    val diaryStrength = "Notes from the engineer's journal: 'Lifted things until my back stopped working. Then lifted more. Garrick says I'm built for this. I think Garrick says that to everyone.'"
    val diaryWisdom = "Notes from the engineer's journal: 'Spent more time reading than building this week. Thistle says the math always wins. I think the math just nags until everyone agrees.'"
    val diaryVitality = "Notes from the engineer's journal: 'Steam works. Salve works. Tonics work. Clover works hardest of all of us, though she'd never say it.'"
    val diarySpirit = "Notes from the engineer's journal: 'Talked more than I worked, somehow, and we still got it done. People are easier to organize than rubble. Marginally.'"

    private val tbcText = """
        The cracks aren't moving. Not yet.

        You stand in the square with the rest of them — Chester with his clipboard finally lowered, Garrick with his arms uncrossed, Thistle with a book pressed flat against his chest, Clover with a satchel that smells like lavender and rain.

        Whatever comes next, it comes for all of you.
    """.trimIndent()

    val nodes: Map<String, StoryNode> = listOf(
        // Section 1 ------------------------------------------------------
        StoryNode(
            id = "s1_intro",
            text = sectionOnePrompt,
            backgroundAsset = BG_DESTROYED,
            characterAssets = listOf(CH_CHESTER, CH_GARRICK),
            requiredLevel = 1,
            choices = listOf(
                StoryChoice("Start with whatever is closest.", null, 0, "s1_default"),
                StoryChoice("Clear the training grounds entrance.", CATEGORY_STRENGTH, DEFAULT_GEM_COST, "s1_strength"),
                StoryChoice("Salvage blueprints from the library.", CATEGORY_WISDOM, DEFAULT_GEM_COST, "s1_wisdom"),
                StoryChoice("Open the path to the hot springs.", CATEGORY_VITALITY, DEFAULT_GEM_COST, "s1_vitality"),
                StoryChoice("Rally the scattered villagers.", CATEGORY_SPIRIT, DEFAULT_GEM_COST, "s1_spirit"),
            ),
        ),
        StoryNode("s1_default", s1FlavorDefault, BG_DESTROYED, listOf(CH_CHESTER, CH_GARRICK), nextNodeId = "s1_post"),
        StoryNode("s1_strength", s1FlavorStrength, BG_DESTROYED, listOf(CH_GARRICK), nextNodeId = "s1_post"),
        StoryNode("s1_wisdom", s1FlavorWisdom, BG_DESTROYED, listOf(CH_THISTLE), nextNodeId = "s1_post"),
        StoryNode("s1_vitality", s1FlavorVitality, BG_DESTROYED, listOf(CH_CLOVER), nextNodeId = "s1_post"),
        StoryNode("s1_spirit", s1FlavorSpirit, BG_DESTROYED, listOf(CH_CHESTER), nextNodeId = "s1_post"),
        StoryNode("s1_post", interlude1, BG_DESTROYED, listOf(CH_CHESTER, CH_GARRICK), nextNodeId = "s2_intro"),

        // Section 2 ------------------------------------------------------
        StoryNode(
            id = "s2_intro",
            text = sectionTwoPrompt,
            backgroundAsset = BG_BUILDING,
            characterAssets = listOf(CH_THISTLE, CH_CHESTER),
            requiredLevel = 2,
            choices = listOf(
                StoryChoice("Add it to Chester's schedule.", null, 0, "s2_default"),
                StoryChoice("Brace the wall with timber now.", CATEGORY_STRENGTH, DEFAULT_GEM_COST, "s2_strength"),
                StoryChoice("Have Thistle calculate the minimum bracing.", CATEGORY_WISDOM, DEFAULT_GEM_COST, "s2_wisdom"),
                StoryChoice("Move the villagers to safety first.", CATEGORY_VITALITY, DEFAULT_GEM_COST, "s2_vitality"),
                StoryChoice("Teach villagers basic bracing.", CATEGORY_SPIRIT, DEFAULT_GEM_COST, "s2_spirit"),
            ),
        ),
        StoryNode("s2_default", s2FlavorDefault, BG_BUILDING, listOf(CH_CHESTER), nextNodeId = "s2_post"),
        StoryNode("s2_strength", s2FlavorStrength, BG_BUILDING, listOf(CH_GARRICK), nextNodeId = "s2_post"),
        StoryNode("s2_wisdom", s2FlavorWisdom, BG_BUILDING, listOf(CH_THISTLE), nextNodeId = "s2_post"),
        StoryNode("s2_vitality", s2FlavorVitality, BG_BUILDING, listOf(CH_CLOVER), nextNodeId = "s2_post"),
        StoryNode("s2_spirit", s2FlavorSpirit, BG_BUILDING, listOf(CH_ASHFORD), nextNodeId = "s2_post"),
        StoryNode("s2_post", interlude2, BG_BUILDING, listOf(CH_THISTLE, CH_CLOVER), nextNodeId = "s3_intro"),

        // Section 3 ------------------------------------------------------
        StoryNode(
            id = "s3_intro",
            text = sectionThreePrompt,
            backgroundAsset = BG_BUILDING,
            characterAssets = listOf(CH_GARRICK, CH_THISTLE),
            requiredLevel = 3,
            choices = listOf(
                StoryChoice("Pull them off by hand.", null, 0, "s3_default"),
                StoryChoice("Cut through with heavy shears.", CATEGORY_STRENGTH, DEFAULT_GEM_COST, "s3_strength"),
                StoryChoice("Mix a solvent in the library.", CATEGORY_WISDOM, DEFAULT_GEM_COST, "s3_wisdom"),
                StoryChoice("Pipe steam from the hot springs.", CATEGORY_VITALITY, DEFAULT_GEM_COST, "s3_vitality"),
                StoryChoice("Pull together as a team.", CATEGORY_SPIRIT, DEFAULT_GEM_COST, "s3_spirit"),
            ),
        ),
        StoryNode("s3_default", s3FlavorDefault, BG_BUILDING, listOf(CH_CHESTER), nextNodeId = "s3_post"),
        StoryNode("s3_strength", s3FlavorStrength, BG_BUILDING, listOf(CH_GARRICK), nextNodeId = "s3_post"),
        StoryNode("s3_wisdom", s3FlavorWisdom, BG_BUILDING, listOf(CH_THISTLE), nextNodeId = "s3_post"),
        StoryNode("s3_vitality", s3FlavorVitality, BG_BUILDING, listOf(CH_CLOVER), nextNodeId = "s3_post"),
        StoryNode("s3_spirit", s3FlavorSpirit, BG_BUILDING, listOf(CH_CHESTER), nextNodeId = "s3_post"),
        StoryNode("s3_post", interlude3, BG_BUILDING, listOf(CH_CHESTER), nextNodeId = "s4_intro"),

        // Section 4 ------------------------------------------------------
        StoryNode(
            id = "s4_intro",
            text = sectionFourPrompt,
            backgroundAsset = BG_HOT_SPRINGS,
            characterAssets = listOf(CH_CLOVER, CH_GARRICK, CH_CHESTER),
            requiredLevel = 4,
            choices = listOf(
                StoryChoice("Let Clover run the springs.", null, 0, "s4_default"),
                StoryChoice("Dig steam trenches toward the vines.", CATEGORY_STRENGTH, DEFAULT_GEM_COST, "s4_strength"),
                StoryChoice("Restore the original pipe network.", CATEGORY_WISDOM, DEFAULT_GEM_COST, "s4_wisdom"),
                StoryChoice("Brew tonics from the spring water.", CATEGORY_VITALITY, DEFAULT_GEM_COST, "s4_vitality"),
                StoryChoice("Set up a communal rest area.", CATEGORY_SPIRIT, DEFAULT_GEM_COST, "s4_spirit"),
            ),
        ),
        StoryNode("s4_default", s4FlavorDefault, BG_HOT_SPRINGS, listOf(CH_CLOVER), nextNodeId = "s4_post"),
        StoryNode("s4_strength", s4FlavorStrength, BG_HOT_SPRINGS, listOf(CH_GARRICK), nextNodeId = "s4_post"),
        StoryNode("s4_wisdom", s4FlavorWisdom, BG_HOT_SPRINGS, listOf(CH_THISTLE), nextNodeId = "s4_post"),
        StoryNode("s4_vitality", s4FlavorVitality, BG_HOT_SPRINGS, listOf(CH_CLOVER), nextNodeId = "s4_post"),
        StoryNode("s4_spirit", s4FlavorSpirit, BG_HOT_SPRINGS, listOf(CH_SABLE), nextNodeId = "s4_post"),
        StoryNode("s4_post", interlude4, BG_HOT_SPRINGS, listOf(CH_SABLE, CH_CLOVER), nextNodeId = "s5_intro"),

        // Section 5 ------------------------------------------------------
        StoryNode(
            id = "s5_intro",
            text = sectionFivePrompt,
            backgroundAsset = BG_BUILT,
            characterAssets = listOf(CH_GARRICK, CH_THISTLE, CH_CHESTER),
            requiredLevel = 5,
            choices = listOf(
                StoryChoice("Sit by the fire and rest.", null, 0, "s5_default"),
                StoryChoice("Walk the perimeter with Garrick.", CATEGORY_STRENGTH, DEFAULT_GEM_COST, "s5_strength"),
                StoryChoice("Study foundation blueprints with Thistle.", CATEGORY_WISDOM, DEFAULT_GEM_COST, "s5_wisdom"),
                StoryChoice("Help Clover prep salves for tomorrow.", CATEGORY_VITALITY, DEFAULT_GEM_COST, "s5_vitality"),
                StoryChoice("Swap stories around the fire.", CATEGORY_SPIRIT, DEFAULT_GEM_COST, "s5_spirit"),
            ),
        ),
        StoryNode("s5_default", s5FlavorDefault, BG_BUILT, listOf(CH_CHESTER), nextNodeId = "s5_post"),
        StoryNode("s5_strength", s5FlavorStrength, BG_BUILT, listOf(CH_GARRICK), nextNodeId = "s5_post"),
        StoryNode("s5_wisdom", s5FlavorWisdom, BG_BUILT, listOf(CH_THISTLE), nextNodeId = "s5_post"),
        StoryNode("s5_vitality", s5FlavorVitality, BG_BUILT, listOf(CH_CLOVER), nextNodeId = "s5_post"),
        StoryNode("s5_spirit", s5FlavorSpirit, BG_BUILT, listOf(CH_ASHFORD), nextNodeId = "s5_post"),
        StoryNode("s5_post", interlude5, BG_BUILT, listOf(CH_CHESTER), nextNodeId = "s6_intro"),

        // Section 6 (cliffhanger, no decision) + TBC ---------------------
        StoryNode(
            id = "s6_intro",
            text = sectionSixPrompt,
            backgroundAsset = BG_BUILT,
            characterAssets = listOf(CH_CHESTER, CH_GARRICK, CH_THISTLE, CH_CLOVER),
            requiredLevel = 6,
            nextNodeId = "s_tbc",
        ),
        StoryNode(
            id = "s_tbc",
            text = tbcText,
            backgroundAsset = BG_BUILT,
            characterAssets = listOf(CH_CHESTER, CH_GARRICK, CH_THISTLE, CH_CLOVER),
            isEnding = true,
        ),
    ).associateBy { it.id }

    fun node(id: String): StoryNode? = nodes[id]

    // Diary entry shown one page before TBC, customized to the dominant
    // category among the player's locked-in choices. Returns null if the
    // player hasn't picked anything categorized at all.
    fun diaryFor(dominantCategory: String?): String = when (dominantCategory) {
        CATEGORY_STRENGTH -> diaryStrength
        CATEGORY_WISDOM -> diaryWisdom
        CATEGORY_VITALITY -> diaryVitality
        CATEGORY_SPIRIT -> diarySpirit
        else -> diaryDefault
    }
}
