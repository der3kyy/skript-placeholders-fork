package io.github.apickledwalrus.skriptplaceholders;

import ch.njol.skript.Skript;
import ch.njol.skript.util.Version;
import io.github.apickledwalrus.skriptplaceholders.placeholder.PlaceholderPlugin;
import io.github.apickledwalrus.skriptplaceholders.placeholder.PlaceholderRegistry;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.skriptlang.skript.addon.SkriptAddon;                     // современный
import org.skriptlang.skript.bukkit.lang.eventvalue.EventValueRegistry;
import org.skriptlang.skript.registration.SyntaxRegistry;

public class SkriptPlaceholders extends JavaPlugin {

	private static SkriptPlaceholders instance;
	private PlaceholderRegistry registry;

	// Публичные реестры для использования в элементах синтаксиса
	public static SyntaxRegistry syntaxRegistry;
	public static EventValueRegistry eventValueRegistry;

	public static SkriptPlaceholders getInstance() {
		if (instance == null)
			throw new IllegalStateException("skript-placeholders has not been initialized yet.");
		return instance;
	}

	@Override
	public void onEnable() {
		Plugin skript = getServer().getPluginManager().getPlugin("Skript");
		if (skript == null || !skript.isEnabled()) {
			getLogger().severe("Could not find Skript. Disabling...");
			getServer().getPluginManager().disablePlugin(this);
			return;
		}
		if (Skript.getVersion().isSmallerThan(new Version(2, 7, 3))) {
			getLogger().severe("Skript version too old. Disabling...");
			getServer().getPluginManager().disablePlugin(this);
			return;
		}
		if (PlaceholderPlugin.getInstalledPlugins().isEmpty()) {
			getLogger().severe("No placeholder plugins found. Disabling...");
			getServer().getPluginManager().disablePlugin(this);
			return;
		}

		instance = this;
		registry = new PlaceholderRegistry(this);

		// Современная регистрация аддона
		SkriptAddon modernAddon = Skript.instance().registerAddon(this.getClass(), "SkriptPlaceholders");
		syntaxRegistry = modernAddon.syntaxRegistry();
		eventValueRegistry = modernAddon.registry(
				EventValueRegistry.class,
				() -> EventValueRegistry.empty(Skript.getInstance())
		);

		// Принудительно загружаем классы элементов, чтобы выполнились их static-блоки
		loadSyntaxClasses();
	}

	private void loadSyntaxClasses() {
		try {
			Class.forName("io.github.apickledwalrus.skriptplaceholders.skript.elements.ExprPlaceholder");
			Class.forName("io.github.apickledwalrus.skriptplaceholders.skript.elements.ExprPlaceholderResult");
			Class.forName("io.github.apickledwalrus.skriptplaceholders.skript.elements.ExprPlaceholderValue");
			Class.forName("io.github.apickledwalrus.skriptplaceholders.skript.elements.ExprRelationalPlaceholderPlayers");
			Class.forName("io.github.apickledwalrus.skriptplaceholders.skript.elements.StructCustomPlaceholder");
		} catch (ClassNotFoundException e) {
			getLogger().severe("Failed to load syntax classes: " + e.getMessage());
			getServer().getPluginManager().disablePlugin(this);
		}
	}

	@Override
	public void onDisable() {
		instance = null;
		registry = null;
	}

	public PlaceholderRegistry getRegistry() {
		return registry;
	}
}