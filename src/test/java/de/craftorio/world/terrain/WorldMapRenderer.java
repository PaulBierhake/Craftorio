package de.craftorio.world.terrain;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * Draws the terrain of the generator from above (docs/screenshots/world-map.png). Not a test: it only runs with
 * {@code CRAFTORIO_RENDER_MAP=1 ./gradlew test --tests '*WorldMapRenderer*'}.
 */
class WorldMapRenderer {
    private static final int SIZE = 1_024;

    @Test
    void render() throws IOException {
        if (!"1".equals(System.getenv("CRAFTORIO_RENDER_MAP"))) {
            return;
        }
        FactoryTerrain terrain = new FactoryTerrain(20260101L);
        BufferedImage image = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_RGB);
        for (int px = 0; px < SIZE; px++) {
            for (int pz = 0; pz < SIZE; pz++) {
                int x = (px - SIZE / 2) * 4;
                int z = (pz - SIZE / 2) * 4;
                int height = terrain.height(x, z);
                int color = switch (height) {
                    case FactoryTerrain.PLATEAU_TWO -> 0x8c7a5c;
                    case FactoryTerrain.PLATEAU_ONE -> 0xb59f6e;
                    case FactoryTerrain.GROUND -> 0x5fa046;
                    case 61 -> 0x4a8fd6;
                    case 60 -> 0x3a76bc;
                    default -> 0x2b5b99;
                };
                double distance = Math.sqrt((double) x * x + (double) z * z);
                if (Math.abs(distance - 192) < 3) {
                    color = 0xffffff;
                }
                image.setRGB(px, pz, color);
            }
        }
        File out = new File("docs/screenshots/world-map.png");
        out.getParentFile().mkdirs();
        ImageIO.write(image, "png", out);
    }
}
