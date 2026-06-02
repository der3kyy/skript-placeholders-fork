package io.github.apickledwalrus.skriptplaceholders.skript.elements;

import ch.njol.skript.ScriptLoader;
import ch.njol.skript.Skript;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.Trigger;
import ch.njol.skript.lang.parser.ParserInstance;
import ch.njol.skript.lang.util.SimpleEvent;
import io.github.apickledwalrus.skriptplaceholders.SkriptPlaceholders;
import io.github.apickledwalrus.skriptplaceholders.placeholder.PlaceholderEvaluator;
import io.github.apickledwalrus.skriptplaceholders.placeholder.PlaceholderPlugin;
import io.github.apickledwalrus.skriptplaceholders.placeholder.PlaceholderRegistry;
import io.github.apickledwalrus.skriptplaceholders.skript.PlaceholderEvent;
import io.github.apickledwalrus.skriptplaceholders.skript.RelationalPlaceholderEvent;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.bukkit.lang.eventvalue.EventValue;
import org.skriptlang.skript.bukkit.lang.eventvalue.EventValueRegistry;
import org.skriptlang.skript.lang.entry.EntryContainer;
import org.skriptlang.skript.lang.script.Script;
import org.skriptlang.skript.lang.structure.Structure;
import org.skriptlang.skript.registration.SyntaxInfo;
import org.skriptlang.skript.registration.SyntaxRegistry;

@Name("Custom Placeholder")
@Description({
		"A structure for creating custom placeholders.",
		"The code will be executed every time the placeholder plugin requests a value for the placeholder."
})
@Examples({
		"placeholderapi placeholder with the prefix \"skriptplaceholders\":",
		"\tif the identifier is \"author\": # Placeholder is \"%skriptplaceholders_author%\"",
		"\t\tset the result to \"APickledWalrus\"",
		"placeholderapi relational placeholder with the prefix \"skriptplaceholders\":",
		"\tif the identifier is \"longer_name\": # Placeholder is \"%rel_skriptplaceholders_longer_name%\"",
		"\t\tif the length of the name of the first player > the length of the name of the second player:",
		"\t\t\tset the result to the name of the first player",
		"\t\telse:",
		"\t\t\tset the result to the name of the second player"
})
@Since("1.0.0, 1.3.0 (MVdWPlaceholderAPI support), 1.7.0 (relational placeholder support)")
public class StructCustomPlaceholder extends Structure implements PlaceholderEvaluator {

	static {
		SyntaxRegistry syntaxRegistry = SkriptPlaceholders.syntaxRegistry;
		syntaxRegistry.register(
				SyntaxRegistry.STRUCTURE,
				SyntaxInfo.Structure.builder(StructCustomPlaceholder.class)
						.addPatterns(
								"(placeholder[ ]api|papi) [:relational] placeholder (with|for) [the] prefix %*string%"
						)
						.build()
		);

		// Регистрация event values через тот же EventValueRegistry
		EventValueRegistry eventValueRegistry = SkriptPlaceholders.eventValueRegistry;
		eventValueRegistry.register(
				EventValue.builder(PlaceholderEvent.class, Player.class)
						.getter(event -> {
							OfflinePlayer player = event.getPlayer();
							return player != null ? player.getPlayer() : null;
						})
						.build()
		);
		eventValueRegistry.register(
				EventValue.builder(PlaceholderEvent.class, OfflinePlayer.class)
						.getter(PlaceholderEvent::getPlayer)
						.build()
		);
	}

	// остальной код без изменений...
	private SectionNode source;
	private PlaceholderRegistry registry;
	private String placeholder;
	private boolean isRelational;
	private Trigger trigger;

	@Override
	public boolean init(Literal<?> @NotNull [] args, int matchedPattern, @NotNull ParseResult parseResult, @Nullable EntryContainer entryContainer) {
		String placeholder = ((Literal<String>) args[0]).getSingle();
		String error = PlaceholderPlugin.PLACEHOLDER_API.validatePrefix(placeholder);
		if (error != null) {
			Skript.error(error);
			return false;
		}
		assert entryContainer != null;
		this.source = entryContainer.getSource();
		this.placeholder = placeholder;
		this.registry = SkriptPlaceholders.getInstance().getRegistry();
		this.isRelational = parseResult.hasTag("relational");
		return true;
	}

	@Override
	public boolean load() {
		ParserInstance parser = getParser();
		Script script = parser.getCurrentScript();

		parser.setCurrentEvent("custom placeholder", isRelational ? RelationalPlaceholderEvent.class : PlaceholderEvent.class);

		trigger = new Trigger(script, parser.getCurrentEventName(), new SimpleEvent(), ScriptLoader.loadItems(source));
		int lineNumber = source.getLine();
		trigger.setLineNumber(lineNumber);
		trigger.setDebugLabel(script + ": line " + lineNumber);

		if (Bukkit.isPrimaryThread()) {
			registry.registerPlaceholder(PlaceholderPlugin.PLACEHOLDER_API, placeholder, this);
		} else {
			Bukkit.getScheduler().runTask(SkriptPlaceholders.getInstance(),
					() -> registry.registerPlaceholder(PlaceholderPlugin.PLACEHOLDER_API, placeholder, this)
			);
		}
		return true;
	}

	@Override
	public void unload() {
		if (Bukkit.isPrimaryThread()) {
			registry.unregisterPlaceholder(PlaceholderPlugin.PLACEHOLDER_API, placeholder, this);
		} else {
			Bukkit.getScheduler().runTask(SkriptPlaceholders.getInstance(),
					() -> registry.unregisterPlaceholder(PlaceholderPlugin.PLACEHOLDER_API, placeholder, this)
			);
		}
	}

	@Override
	public @NotNull String toString(@Nullable Event event, boolean debug) {
		return "placeholderapi " + (isRelational ? "relational " : "") + "placeholder with the prefix " + placeholder;
	}

	@Override
	public @Nullable String evaluate(String placeholder, @Nullable OfflinePlayer player) {
		if (isRelational) return null;
		PlaceholderEvent event = new PlaceholderEvent(placeholder, player);
		trigger.execute(event);
		return event.getResult();
	}

	@Override
	public @Nullable String evaluateRelational(String placeholder, Player one, Player two) {
		if (!isRelational) return null;
		RelationalPlaceholderEvent event = new RelationalPlaceholderEvent(placeholder, one, two);
		trigger.execute(event);
		return event.getResult();
	}
}