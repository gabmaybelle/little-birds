package com.gabbymaybelle.littlebirds.gametest.neoforge;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.gamerules.GameRuleMap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

/**
 * Little Birds' NeoForge game tests: a little mod of its own that only {@code :neoforge:runGameTestServer}
 * loads, handing vanilla's test framework one test per check (the checks are in {@link BudgieChecks}).
 * The server has no window, so these run anywhere.
 */
@Mod(LittleBirdsGameTests.MOD_ID)
public final class LittleBirdsGameTests {
	static final String MOD_ID = "little_birds_gametest";
	/** Vanilla's 1x1x1 template, for tests that build nothing. */
	private static final Identifier EMPTY = Identifier.withDefaultNamespace("empty");
	/** An empty 7x5x7 room of the mod's own (tools/make-test-room.py), for a test where a bird moves about: everything stays inside the test's loaded, ticking bounds. */
	private static final Identifier ROOM = id("room");

	/** The test functions by name; the test instance of the same name runs each. */
	private static final Map<String, Consumer<GameTestHelper>> FUNCTIONS = new LinkedHashMap<>();

	static {
		FUNCTIONS.put("loaded", BudgieChecks::loaded);
		FUNCTIONS.put("budgie_lives", BudgieChecks::lives);
		FUNCTIONS.put("seeds_tame_a_budgie", BudgieChecks::tames);
		FUNCTIONS.put("a_budgie_sleeps_on_its_perch", BudgieChecks::roosts);
		FUNCTIONS.put("a_budgie_greets_its_owner", BudgieChecks::greets);
		FUNCTIONS.put("a_budgie_rides_a_shoulder", BudgieChecks::shoulder);
		FUNCTIONS.put("a_perch_seats_one_bird", BudgieChecks::oneSeat);
		FUNCTIONS.put("wild_budgies_follow_the_settings", BudgieChecks::spawnRules);
		FUNCTIONS.put("savannas_and_jungles_spawn_budgies", BudgieChecks::biomes);
		FUNCTIONS.put("a_cockatiel_lives_and_tames", BudgieChecks::cockatiel);
		FUNCTIONS.put("lovebirds_pair_and_share_a_perch", BudgieChecks::lovebirds);
		FUNCTIONS.put("fed_budgies_make_a_chick", BudgieChecks::breeding);
		FUNCTIONS.put("a_lovebird_pair_makes_a_chick", BudgieChecks::lovebirdChick);
		FUNCTIONS.put("treats_snacks_and_a_no_thank_you", BudgieChecks::eating);
		FUNCTIONS.put("a_canary_sings_in_the_morning", BudgieChecks::canary);
		FUNCTIONS.put("zebra_finches_keep_company_and_answer", BudgieChecks::zebraFinches);
	}

	public LittleBirdsGameTests(IEventBus modBus) {
		modBus.addListener(RegisterEvent.class, LittleBirdsGameTests::registerFunctions);
		modBus.addListener(RegisterGameTestsEvent.class, LittleBirdsGameTests::registerTests);
	}

	static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	/** Test functions live in a built-in registry, so they go in with the mod's other registrations. */
	private static void registerFunctions(RegisterEvent event) {
		if (event.getRegistryKey().equals(Registries.TEST_FUNCTION)) {
			FUNCTIONS.forEach((name, function) -> event.register(Registries.TEST_FUNCTION, id(name), () -> function));
		}
	}

	/** Test instances and their environments are registered as the test world's data loads. */
	private static void registerTests(RegisterGameTestsEvent event) {
		Holder<TestEnvironmentDefinition<?>> plain = event.registerEnvironment(id("plain"), new TestEnvironmentDefinition.SetGameRules(GameRuleMap.of()));
		test(event, "loaded", plain, EMPTY, 20);
		test(event, "budgie_lives", plain, EMPTY, 100);
		test(event, "seeds_tame_a_budgie", plain, EMPTY, 40);
		test(event, "a_budgie_sleeps_on_its_perch", plain, ROOM, 1600);
		test(event, "a_budgie_greets_its_owner", plain, ROOM, 200);
		test(event, "a_budgie_rides_a_shoulder", plain, ROOM, 1600);
		test(event, "a_perch_seats_one_bird", plain, ROOM, 1200);
		test(event, "wild_budgies_follow_the_settings", plain, EMPTY, 20);
		test(event, "savannas_and_jungles_spawn_budgies", plain, EMPTY, 20);
		test(event, "a_cockatiel_lives_and_tames", plain, EMPTY, 40);
		test(event, "lovebirds_pair_and_share_a_perch", plain, ROOM, 2400);
		test(event, "fed_budgies_make_a_chick", plain, ROOM, 1200);
		test(event, "a_lovebird_pair_makes_a_chick", plain, ROOM, 2400);
		test(event, "treats_snacks_and_a_no_thank_you", plain, EMPTY, 40);
		// The canary's test has an environment of its own (the same rules, another name), so it runs in
		// a batch of its own and can set the clock to morning without waking the night tests above.
		Holder<TestEnvironmentDefinition<?>> morning = event.registerEnvironment(id("morning"), new TestEnvironmentDefinition.SetGameRules(GameRuleMap.of()));
		test(event, "a_canary_sings_in_the_morning", morning, ROOM, 600);
		test(event, "zebra_finches_keep_company_and_answer", morning, ROOM, 2400);
	}

	private static void test(RegisterGameTestsEvent event, String name, Holder<TestEnvironmentDefinition<?>> environment, Identifier structure, int maxTicks) {
		ResourceKey<Consumer<GameTestHelper>> function = ResourceKey.create(Registries.TEST_FUNCTION, id(name));
		event.registerTest(id(name), new FunctionGameTestInstance(function, new TestData<>(environment, structure, maxTicks, 0, true)));
	}
}
