package util;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Robot;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Pure Java JDK HTTP preview bridge used when launching in preview/dev mode (--preview-port=3000).
 * Serves a live PNG capture of the running Java AWT Frame without introducing any web framework.
 */
public final class PreviewHttpBridge {

    private PreviewHttpBridge() {
    }

    public static void start(int port, AtomicReference<Frame> frameRef) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", port), 0);
        server.createContext("/", exchange -> handleRequest(exchange, frameRef));
        server.setExecutor(Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "awt-preview-http");
            t.setDaemon(true);
            return t;
        }));
        server.start();
        System.out.println("Java AWT preview bridge listening on http://0.0.0.0:" + port);
    }

    private static void handleRequest(HttpExchange exchange, AtomicReference<Frame> frameRef) throws IOException {
        String method = exchange.getRequestMethod();
        byte[] pngBytes = captureFrameAsPng(frameRef.get());

        exchange.getResponseHeaders().set("Content-Type", "image/png");
        exchange.getResponseHeaders().set("Cache-Control", "no-store, no-cache, must-revalidate");
        if ("HEAD".equalsIgnoreCase(method)) {
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
            return;
        }
        exchange.sendResponseHeaders(200, pngBytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(pngBytes);
        }
    }

    private static byte[] captureFrameAsPng(Frame frame) throws IOException {
        BufferedImage image = null;
        if (frame != null && frame.isShowing() && frame.getWidth() > 0 && frame.getHeight() > 0) {
            final AtomicReference<Rectangle> boundsRef = new AtomicReference<>();
            try {
                EventQueue.invokeAndWait(() -> {
                    if (frame.isShowing()) {
                        boundsRef.set(frame.getBounds());
                    }
                });
            } catch (Exception ignored) {
                // Fall back to direct bounds
                boundsRef.set(frame.getBounds());
            }

            Rectangle bounds = boundsRef.get();
            if (bounds != null && bounds.width > 0 && bounds.height > 0) {
                try {
                    Robot robot = new Robot();
                    image = robot.createScreenCapture(bounds);
                } catch (Exception ignored) {
                    image = new BufferedImage(bounds.width, bounds.height, BufferedImage.TYPE_INT_RGB);
                    Graphics2D g2d = image.createGraphics();
                    try {
                        frame.paintAll(g2d);
                    } finally {
                        g2d.dispose();
                    }
                }
            }
        }

        if (image == null) {
            image = createStartupSplashImage();
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        return baos.toByteArray();
    }

    private static BufferedImage createStartupSplashImage() {
        BufferedImage img = new BufferedImage(960, 540, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = img.createGraphics();
        try {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setColor(new Color(0x12, 0x12, 0x14));
            g2d.fillRect(0, 0, 960, 540);
            g2d.setColor(new Color(0x8B, 0x5C, 0xF6));
            g2d.setFont(new Font("SansSerif", Font.BOLD, 24));
            g2d.drawString("Stock Price Predictor — Java AWT Desktop App", 48, 245);
            g2d.setColor(new Color(0x9A, 0x9A, 0xA5));
            g2d.setFont(new Font("SansSerif", Font.PLAIN, 15));
            g2d.drawString("CSE 213 Advanced Java Programming | SRM University AP", 48, 280);
            g2d.drawString("Initializing AWT window and SQLite JDBC database...", 48, 310);
        } finally {
            g2d.dispose();
        }
        return img;
    }
}
