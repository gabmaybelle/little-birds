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

	/** The test functions by name; the test instance of the same name runs each. */
	private static final Map<String, Consumer<GameTestHelper>> FUNCTIONS = new LinkedHashMap<>();

	static {
		FUNCTIONS.put("loaded", BudgieChecks::loaded);
		FUNCTIONS.put("budgie_lives", BudgieChecks::lives);
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
	}

	private static void test(RegisterGameTestsEvent event, String name, Holder<TestEnvironmentDefinition<?>> environment, Identifier structure, int maxTicks) {
		ResourceKey<Consumer<GameTestHelper>> function = ResourceKey.create(Registries.TEST_FUNCTION, id(name));
		event.registerTest(id(name), new FunctionGameTestInstance(function, new TestData<>(environment, structure, maxTicks, 0, true)));
	}
}
