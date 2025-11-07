package org.acme;

import io.quarkus.logging.Log;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RadialGradientPaint;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Path("/sunset")
public class SunsetResource {

    public static final AtomicBoolean rising = new AtomicBoolean(true);
    public static final AtomicInteger offset = new AtomicInteger(0);

    private static final int width = 800;
    private static final int height = 600;
    private static final int sunSize = width / 3;
    private static final int sunX = width / 2 - sunSize / 2;

    private final BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_INDEXED);
    private final Random random = new Random();

    private final Color[] sunsetColors = {
            new Color(255, 100, 0),
            new Color(255, 50, 0),
            new Color(200, 0, 50),
            new Color(150, 0, 120)
    };

    private final Color[] seaColors = {
            new Color(0, 50, 150),
            new Color(0, 100, 200),
            new Color(0, 150, 255)
    };

    @GET
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public byte[] generateSunsetImage() {
        final Graphics2D g2d = image.createGraphics();
        // Sky
        final int skyHeight = height / 2;
        for (int i = 0; i < skyHeight; i++) {
            g2d.setColor(sunsetColors[random.nextInt(sunsetColors.length)]);
            g2d.drawLine(0, i, width, i);
        }
        // Sun
        if (rising.get()) {
            if (offset.decrementAndGet() < Math.negateExact(sunSize)) {
                rising.set(false);
            }
        } else {
            if (offset.incrementAndGet() > 10) {
                rising.set(true);
            }
        }
        final int sunY = height / 2 - 20 + offset.get();
        g2d.setColor(Color.YELLOW);
        final float[] gradientFractions = { 0.1f, 1.0f };
        final Color[] gradientColors = { sunsetColors[2], Color.YELLOW };
        g2d.setPaint(
                new RadialGradientPaint(new Point(sunX, sunY + offset.get() * 2), sunSize * 2,
                        gradientFractions, gradientColors));
        g2d.fillOval(sunX, sunY, sunSize, sunSize);
        // Sea
        for (int i = height / 2; i < height; i++) {
            g2d.setColor(seaColors[random.nextInt(seaColors.length)]);
            g2d.drawLine(0, i, width, i);
        }
        g2d.dispose();
        try (final ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(image, "GIF", baos);
            return baos.toByteArray();
        } catch (IOException e) {
            Log.error("Image generation failed: " + e.getMessage(), e);
            return new byte[0];
        }
    }
}
