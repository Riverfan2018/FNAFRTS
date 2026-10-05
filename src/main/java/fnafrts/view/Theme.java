package fnafrts.view;

import java.awt.Color;
import java.awt.Font;

public final class Theme {
    private Theme() {}

    // ---------- Fondos ----------
    public static final Color BG_DEEP             = new Color(10, 10, 16);
    public static final Color BG_PANEL            = new Color(20, 20, 30);
    public static final Color BG_SECTOR_ACTIVE    = new Color(38, 42, 60, 220);
    public static final Color BG_SECTOR_DIM       = new Color(16, 16, 24, 140);

    // ---------- Bordes de sector ----------
    public static final Color BORDER_SECTOR_ACTIVE = new Color(120, 140, 190);
    public static final Color BORDER_SECTOR_DIM    = new Color(50, 50, 70);

    // ---------- Links ----------
    public static final Color LINK_ACTIVE = new Color(140, 150, 190);
    public static final Color LINK_DIM    = new Color(45, 45, 60);

    // ---------- Nodos ----------
    public static final Color NODE_NORMAL        = new Color(52, 56, 82);
    public static final Color NODE_HOME          = new Color(60, 100, 70);
    public static final Color NODE_RESPAWN       = new Color(110, 60, 60);
    public static final Color NODE_ENTRY         = new Color(120, 90, 40);
    public static final Color NODE_BORDER_ACTIVE = new Color(180, 190, 220);
    public static final Color NODE_BORDER_DIM    = new Color(60, 60, 80);
    public static final Color NODE_SHADOW        = new Color(0, 0, 0, 120);
    public static final Color NODE_OFFICE          = new Color(95, 70, 160);
    public static final Color NODE_OFFICE_BORDER   = new Color(180, 150, 240);
    public static final Color NODE_OFFICE_GLOW     = new Color(150, 120, 230);
    public static final Color BG_SECTOR_OFFICE     = new Color(60, 45, 100, 200);

    // ---------- Texto ----------
    public static final Color TEXT_PRIMARY = new Color(230, 230, 240);
    public static final Color TEXT_MUTED   = new Color(130, 130, 150);

    // ---------- Fuentes ----------
    public static final Font FONT_TITLE = new Font("SansSerif", Font.BOLD, 16);
    public static final Font FONT_SECTOR_LABEL   = new Font("SansSerif", Font.BOLD, 11);
    public static final Font FONT_ANIMATRONIC    = new Font("SansSerif", Font.BOLD, 12);
    public static final Font FONT_BUTTON         = new Font("SansSerif", Font.PLAIN, 13);

    // ---------- Puertas ----------
    public static final Color DOOR_OPEN      = new Color(80, 180, 100);
    public static final Color DOOR_CLOSED    = new Color(200, 70, 70);
    public static final Color DOOR_OVERHEAT  = new Color(230, 140, 40);

    // ---------- Luces de puerta ----------
    public static final Color NODE_ENTRY_LIT   = new Color(190, 150, 50);
    public static final Color ENTRY_GLOW       = new Color(255, 215, 100);
    public static final Color LINK_ENTRY       = new Color(170, 135, 60);

    // ---------- Calor ----------
    public static final Color HEAT_LOW    = new Color(80, 180, 100);
    public static final Color HEAT_MEDIUM = new Color(220, 200, 80);
    public static final Color HEAT_HIGH   = new Color(230, 140, 40);
    public static final Color HEAT_CRIT   = new Color(200, 60, 60);

    // ---------- Atención ----------
    public static final Color ATTENTION_LOW  = new Color(80, 200, 220);   // cyan
    public static final Color ATTENTION_MED  = new Color(140, 140, 230);  // violeta
    public static final Color ATTENTION_HIGH = new Color(220, 90, 180);   // magenta

    // ---------- Peek ----------
    public static final Color PEEK_DARK = new Color(12, 12, 18);
    public static final Color PEEK_LIT  = new Color(40, 45, 55);

    // ---------- Helpers ----------

    public static Color dim(Color c, float factor) {
        return new Color(
                (int)(c.getRed()   * factor),
                (int)(c.getGreen() * factor),
                (int)(c.getBlue()  * factor),
                c.getAlpha());
    }

    public static Color withAlpha(Color c, int alpha) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
    }
}