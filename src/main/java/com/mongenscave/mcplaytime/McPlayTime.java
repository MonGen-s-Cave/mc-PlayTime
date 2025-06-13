package com.mongenscave.mcplaytime;

import com.github.Anon8281.universalScheduler.UniversalScheduler;
import com.github.Anon8281.universalScheduler.scheduling.schedulers.TaskScheduler;
import com.mongenscave.mcplaytime.config.Config;
import com.mongenscave.mcplaytime.database.MySQL;
import com.mongenscave.mcplaytime.hooks.plugins.PlaceholderAPI;
import com.mongenscave.mcplaytime.listener.PlayerListener;
import com.mongenscave.mcplaytime.service.OnlineTimeService;
import com.mongenscave.mcplaytime.utils.LoggerUtils;
import com.mongenscave.mcplaytime.utils.RegisterUtils;
import dev.dejvokep.boostedyaml.settings.dumper.DumperSettings;
import dev.dejvokep.boostedyaml.settings.general.GeneralSettings;
import dev.dejvokep.boostedyaml.settings.loader.LoaderSettings;
import dev.dejvokep.boostedyaml.settings.updater.UpdaterSettings;
import lombok.Getter;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import revxrsal.zapper.ZapperJavaPlugin;

import java.io.File;
import java.sql.SQLException;

public final class McPlayTime extends ZapperJavaPlugin {
    @Getter private static McPlayTime instance;
    @Getter private Config language;
    @Getter private TaskScheduler scheduler;
    @Getter private OnlineTimeService service;
    @Getter private MySQL database;

    private Config config;

    @Override
    public void onLoad() {
        instance = this;
        scheduler = UniversalScheduler.getScheduler(this);
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();

        try {
            initializeComponents();
        } catch (SQLException exception) {
            LoggerUtils.error(exception.getMessage());
        }

        getServer().getPluginManager().registerEvents(new PlayerListener(), this);

        RegisterUtils.registerCommands();
        PlaceholderAPI.registerHook();

        startPeriodicSave();
        LoggerUtils.printStartup();
    }

    @Override
    public void onDisable() {
        try {
            if (service != null) service.saveAllOnlinePlayers();
            if (database != null) database.close();
            if (scheduler != null) scheduler.cancelTasks();
        } catch (Exception exception) {
            LoggerUtils.error(exception.getMessage());
        }
    }

    public Config getConfiguration() {
        return config;
    }

    private void initializeComponents() throws SQLException {
        final GeneralSettings generalSettings = GeneralSettings.builder()
                .setUseDefaults(false)
                .build();

        final LoaderSettings loaderSettings = LoaderSettings.builder()
                .setAutoUpdate(true)
                .build();

        final UpdaterSettings updaterSettings = UpdaterSettings.builder()
                .setKeepAll(true)
                .build();

        config = loadConfig("config.yml", generalSettings, loaderSettings, updaterSettings);
        language = loadConfig("messages.yml", generalSettings, loaderSettings, updaterSettings);

        database = new MySQL();
        service = new OnlineTimeService();

        database.initialize();
    }

    @NotNull
    @Contract("_, _, _, _ -> new")
    private Config loadConfig(@NotNull String fileName, @NotNull GeneralSettings generalSettings, @NotNull LoaderSettings loaderSettings, @NotNull UpdaterSettings updaterSettings) {
        return new Config(
                new File(getDataFolder(), fileName),
                getResource(fileName),
                generalSettings,
                loaderSettings,
                DumperSettings.DEFAULT,
                updaterSettings
        );
    }

    private void startPeriodicSave() {
        scheduler.runTaskTimerAsynchronously(() -> {
            if (service != null) service.saveAllOnlinePlayers();
        }, 20L * 60L, 20L * 60L);
    }
}
