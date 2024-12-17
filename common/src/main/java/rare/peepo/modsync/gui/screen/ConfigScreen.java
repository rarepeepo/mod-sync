package rare.peepo.modsync.gui.screen;

import java.net.URL;
import java.util.concurrent.Executors;
import net.minecraft.SharedConstants;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.TextWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import rare.peepo.modsync.Log;
import rare.peepo.modsync.ModSync;
import rare.peepo.modsync.Util;
import rare.peepo.modsync.config.Config;
import rare.peepo.modsync.config.SyncAction;
import rare.peepo.modsync.config.SyncConfig;

public class ConfigScreen extends Screen {
    final Screen parent;
    TextFieldWidget urlField;
    CyclingButtonWidget<Object> actionButton;
    final int major, minor;

    public ConfigScreen(Screen parent) {
        super(_t("labelSettings"));
        this.parent = parent;

        var p = SharedConstants.getGameVersion().getName().split("\\.");
        major = Integer.parseInt(p[1]);
        minor = p.length > 2 ? Integer.parseInt(p[2]) : 0;
    }
    
    @Override
    protected void init() {
        super.init();
        final var x = width / 2 - 154;
        
        addDrawableChild(
                new TextWidget(x, 35, 250, 20, _t("labelSyncUrl"), textRenderer).alignLeft()
        );
        urlField = new TextFieldWidget(this.textRenderer, x, 55, 310, 20, Text.empty());
        urlField.setMaxLength(1024);
        urlField.setTooltip(
                Tooltip.of(_t("urlField.tooltip"))
        );
        if (Config.Url != null)
            urlField.setText(Config.Url.toString());
        addDrawableChild(urlField);

        var statusText = new TextWidget(x + 85, 82, 250, 20, Text.empty(), textRenderer).alignLeft();
        addDrawableChild(statusText);
        
        var testButton = ButtonWidget.builder(_t("testButton"), button -> {
            statusText.setMessage(Text.empty());
            var s = urlField.getText().trim();
            if (!s.isEmpty())
                testUrl(s, button, statusText);
        }).dimensions(x - 1, 80, 75, 20).build();
        
        testButton.setTooltip(Tooltip.of(_t("testButton.tooltip")));
        addDrawableChild(testButton);
        
        addDrawableChild(
                new TextWidget(x, 110, 250, 20, _t("labelSyncAction"), textRenderer).alignLeft()
        );
        
        actionButton = CyclingButtonWidget
                .builder(value -> _t("actionButton." + value.toString().toLowerCase()))
                .values((Object[])SyncAction.values())
                .initially(Config.Action)
                .build(x, 130, 120, 20, _t("actionButton"), (b, value) -> {
                    Log.info(value);
                    b.setTooltip(Tooltip.of(
                            _t("actionButton." + value.toString().toLowerCase() + ".tooltip"))
                    );
                });
        actionButton.setTooltip(Tooltip.of(
                _t("actionButton." + Config.Action.toString().toLowerCase() + ".tooltip"))
        );
        addDrawableChild(actionButton);
        
        addDrawableChild(ButtonWidget
                .builder(ScreenTexts.CANCEL, b -> close(false))
                .dimensions(this.width / 2 - 154, this.height - 26, 150, 20)
                .build());
        
        addDrawableChild(ButtonWidget
                .builder(ScreenTexts.DONE, b -> close(true))
                .dimensions(this.width / 2 + 4, this.height - 26, 150, 20).build());
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Older versions (1.20 and 1.20.1) don't call this in their render() method
        // for whatever reason, while newer versions (1.20.2 and up) do, while
        // even newer versions (1.20.5) don't have the renderBackgroundTexture method
        // at all...fun times.
        if (major < 21 && minor < 2)
            this.renderBackgroundTexture(context);
        // Oh and even older versions (before 1.20) don't even have DrawContext
        // so it's a complete mess to support them and would require a separate
        // git branch and frankly I can't be bothered.
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 15,
                0xFFFFFF);
    }

    @Override
    public void close() {
      client.setScreen(parent);
    }
    
    void close(boolean saveSettings) {
        if (saveSettings)
            saveSettings();
        close();
    }
    
    static MutableText _t(String key) {
        return Text.translatable(ModSync.ID + "." + key);
    }
    
    void testUrl(String url, ButtonWidget testButton, TextWidget status) {
        status.setMessage(_t("queryingSyncUrl"));
        status.setTextColor(Formatting.YELLOW.getColorValue());
        testButton.active = false;
        // No idea if it is save to modify the GUI from a worker thread.
        Executors.newCachedThreadPool().execute(() -> {
            try {
                // Give user a chance to see query label so they understand
                // something is happening when they click the button.
                Thread.sleep(500);
                SyncConfig.parse(
                        Util.downloadString(new URL(url))
                );
                status.setMessage(_t("syncUrlIsWorking"));
                status.setTextColor(Formatting.GREEN.getColorValue());
            } catch (Exception e) {
                Log.error("testUrl: error querying URL '{}': {}", url, e);
                status.setMessage(_t("syncUrlIsNotWorking"));
                status.setTextColor(Formatting.RED.getColorValue());
            } finally {
                testButton.active = true;
            }
        });
    }
    
    void saveSettings() {
        try {
            Config.Action = (SyncAction)actionButton.getValue();
            var s = urlField.getText().trim();
            if(s.isEmpty())
                Config.Url = null;
            else
                Config.Url = new URL(s);
            Log.info("Saving config settings");
            Config.save();
        } catch (Exception e) {
            Log.error("Error saving config settings: {}", e);
        }
    }
}
