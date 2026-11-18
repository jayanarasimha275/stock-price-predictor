package ui;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Lightweight JDK HttpServer bridge activated ONLY when `--preview-port=<port>` is passed.
 * Streams the live Java AWT Frame/Dialog pixels via java.awt.Robot and forwards pointer/key events
 * so the real AWT desktop window is interactive in container preview environments without modifying
 * native desktop execution (`mvn exec:java`).
 */
public final class PreviewServer {

    private final HttpServer server;
    private final Robot robot;

    private PreviewServer(int port) throws IOException, AWTException {
        this.robot = new Robot();
        this.robot.setAutoDelay(12);
        this.server = HttpServer.create(new InetSocketAddress("0.0.0.0", port), 0);
        this.server.setExecutor(Executors.newCachedThreadPool());
        registerRoutes();
        this.server.start();
    }

    public static PreviewServer start(int port) {
        try {
            PreviewServer ps = new PreviewServer(port);
            System.out.println("Java AWT Preview Bridge listening on http://0.0.0.0:" + port);
            return ps;
        } catch (IOException | AWTException ex) {
            System.err.println("Failed to start preview bridge on port " + port + ": " + ex.getMessage());
            return null;
        }
    }

    private void registerRoutes() {
        server.createContext("/health", exchange -> {
            byte[] ok = "{\"status\":\"ok\",\"runtime\":\"java-awt\"}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            exchange.sendResponseHeaders(200, ok.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(ok);
            }
        });

        server.createContext("/frame.png", exchange -> {
            try {
                Window target = resolveActiveWindow();
                if (target == null || !target.isShowing() || target.getWidth() <= 0 || target.getHeight() <= 0) {
                    byte[] blank = renderPlaceholderPng("Starting Stock Price Predictor (Java 17 AWT)...");
                    sendBytes(exchange, 200, "image/png", blank);
                    return;
                }
                Rectangle bounds = resolveCaptureBounds(target);
                BufferedImage shot = robot.createScreenCapture(bounds);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(shot, "png", baos);
                sendBytes(exchange, 200, "image/png", baos.toByteArray());
            } catch (Exception ex) {
                byte[] blank = renderPlaceholderPng("AWT Window Initializing...");
                sendBytes(exchange, 200, "image/png", blank);
            }
        });

        server.createContext("/click", exchange -> {
            Map<String, String> params = parseQuery(exchange.getRequestURI().getRawQuery());
            int x = parseInt(params.get("x"), -1);
            int y = parseInt(params.get("y"), -1);
            Window target = resolveActiveWindow();
            if (target != null && x >= 0 && y >= 0) {
                Rectangle b = resolveCaptureBounds(target);
                int sx = b.x + x;
                int sy = b.y + y;
                try {
                    EventQueue.invokeAndWait(() -> target.toFront());
                } catch (Exception ignored) {
                }
                robot.mouseMove(sx, sy);
                robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
                robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
            }
            sendBytes(exchange, 200, "text/plain", "ok".getBytes(StandardCharsets.UTF_8));
        });

        server.createContext("/wheel", exchange -> {
            Map<String, String> params = parseQuery(exchange.getRequestURI().getRawQuery());
            int x = parseInt(params.get("x"), -1);
            int y = parseInt(params.get("y"), -1);
            int delta = parseInt(params.get("dy"), 0);
            Window target = resolveActiveWindow();
            if (target != null && delta != 0) {
                Rectangle b = resolveCaptureBounds(target);
                if (x >= 0 && y >= 0) {
                    robot.mouseMove(b.x + x, b.y + y);
                }
                robot.mouseWheel(delta > 0 ? 3 : -3);
            }
            sendBytes(exchange, 200, "text/plain", "ok".getBytes(StandardCharsets.UTF_8));
        });

        server.createContext("/key", exchange -> {
            String body = readBody(exchange.getRequestBody());
            Map<String, String> params = parseQuery(body);
            String key = params.getOrDefault("key", "");
            handleKeyInput(key);
            sendBytes(exchange, 200, "text/plain", "ok".getBytes(StandardCharsets.UTF_8));
        });

        server.createContext("/", exchange -> {
            byte[] doc = VIEWER_DOCUMENT.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Cache-Control", "no-store");
            sendBytes(exchange, 200, "text/html; charset=UTF-8", doc);
        });
    }

    private void handleKeyInput(String key) {
        if (key == null || key.isEmpty()) {
            return;
        }
        switch (key) {
            case "Enter" -> pressAndRelease(KeyEvent.VK_ENTER);
            case "Backspace" -> handleBackspace();
            case "Tab" -> pressAndRelease(KeyEvent.VK_TAB);
            case "Escape" -> pressAndRelease(KeyEvent.VK_ESCAPE);
            case "ArrowUp" -> pressAndRelease(KeyEvent.VK_UP);
            case "ArrowDown" -> pressAndRelease(KeyEvent.VK_DOWN);
            case "ArrowLeft" -> pressAndRelease(KeyEvent.VK_LEFT);
            case "ArrowRight" -> pressAndRelease(KeyEvent.VK_RIGHT);
            default -> {
                if (key.length() == 1) {
                    typeCharacterSafely(key.charAt(0));
                }
            }
        }
    }

    private void handleBackspace() {
        final boolean[] handledDirectly = {false};
        try {
            EventQueue.invokeAndWait(() -> {
                Component focus = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
                if (focus instanceof TextComponent tc && tc.isEditable()) {
                    String txt = tc.getText();
                    int selStart = tc.getSelectionStart();
                    int selEnd = tc.getSelectionEnd();
                    if (selEnd > selStart) {
                        tc.setText(txt.substring(0, selStart) + txt.substring(selEnd));
                        tc.setCaretPosition(selStart);
                    } else if (selStart > 0 && selStart <= txt.length()) {
                        tc.setText(txt.substring(0, selStart - 1) + txt.substring(selStart));
                        tc.setCaretPosition(selStart - 1);
                    }
                    handledDirectly[0] = true;
                }
            });
        } catch (Exception ignored) {
        }
        if (!handledDirectly[0]) {
            pressAndRelease(KeyEvent.VK_BACK_SPACE);
        }
    }

    private void typeCharacterSafely(char ch) {
        final boolean[] inserted = {false};
        try {
            EventQueue.invokeAndWait(() -> {
                Component focus = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
                if (focus instanceof TextComponent tc && tc.isEditable() && !Character.isISOControl(ch)) {
                    String txt = tc.getText();
                    int selStart = Math.max(0, Math.min(txt.length(), tc.getSelectionStart()));
                    int selEnd = Math.max(selStart, Math.min(txt.length(), tc.getSelectionEnd()));
                    String updated = txt.substring(0, selStart) + ch + txt.substring(selEnd);
                    tc.setText(updated);
                    tc.setCaretPosition(selStart + 1);
                    inserted[0] = true;
                }
            });
        } catch (Exception ignored) {
        }
        if (!inserted[0]) {
            int code = KeyEvent.getExtendedKeyCodeForChar(ch);
            if (code != KeyEvent.VK_UNDEFINED) {
                try {
                    boolean upper = Character.isUpperCase(ch);
                    if (upper) robot.keyPress(KeyEvent.VK_SHIFT);
                    robot.keyPress(code);
                    robot.keyRelease(code);
                    if (upper) robot.keyRelease(KeyEvent.VK_SHIFT);
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
    }

    private void pressAndRelease(int keyCode) {
        robot.keyPress(keyCode);
        robot.keyRelease(keyCode);
    }

    private Window resolveActiveWindow() {
        AtomicReference<Window> ref = new AtomicReference<>();
        try {
            EventQueue.invokeAndWait(() -> {
                Window[] windows = Window.getWindows();
                // Priority 1: Visible modal Dialog (AdminSetupDialog, ConfirmDialog, HowItWorksDialog)
                for (int i = windows.length - 1; i >= 0; i--) {
                    if (windows[i] instanceof Dialog d && d.isShowing()) {
                        ref.set(d);
                        return;
                    }
                }
                // Priority 2: Active showing Frame (LoginFrame, RegisterFrame, MainFrame, AdminDashboard)
                for (int i = windows.length - 1; i >= 0; i--) {
                    if (windows[i].isShowing()) {
                        ref.set(windows[i]);
                        return;
                    }
                }
            });
        } catch (Exception ignored) {
        }
        return ref.get();
    }

    private Rectangle resolveCaptureBounds(Window target) {
        Rectangle bounds = target.getBounds();
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        int x = Math.max(0, bounds.x);
        int y = Math.max(0, bounds.y);
        int w = Math.min(Math.max(100, bounds.width), Math.max(100, screen.width - x));
        int h = Math.min(Math.max(100, bounds.height), Math.max(100, screen.height - y));
        return new Rectangle(x, y, w, h);
    }

    private byte[] renderPlaceholderPng(String msg) throws IOException {
        BufferedImage img = new BufferedImage(900, 560, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = img.createGraphics();
        g2.setColor(new Color(0x12, 0x12, 0x14));
        g2.fillRect(0, 0, 900, 560);
        g2.setColor(new Color(0x8B, 0x5C, 0xF6));
        g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(msg, (900 - fm.stringWidth(msg)) / 2, 280);
        g2.dispose();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", baos);
        return baos.toByteArray();
    }

    private void sendBytes(HttpExchange exchange, int code, String contentType, byte[] body) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.sendResponseHeaders(code, body.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(body);
        }
    }

    private String readBody(InputStream in) throws IOException {
        return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }

    private Map<String, String> parseQuery(String q) {
        Map<String, String> map = new LinkedHashMap<>();
        if (q == null || q.isEmpty()) return map;
        for (String pair : q.split("&")) {
            int idx = pair.indexOf('=');
            if (idx > 0) {
                String k = URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8);
                String v = URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8);
                map.put(k, v);
            }
        }
        return map;
    }

    private int parseInt(String raw, int fallback) {
        try {
            return raw != null ? Integer.parseInt(raw.trim()) : fallback;
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private static final String VIEWER_DOCUMENT = """
            <!doctype html>
            <html lang="en">
            <head>
              <meta charset="utf-8" />
              <title>Stock Price Predictor — Java AWT Desktop Preview</title>
              <style>
                html, body { margin:0; padding:0; width:100%; height:100%; background:#121214; display:flex; align-items:center; justify-content:center; overflow:hidden; font-family:sans-serif; }
                #awt { max-width:100vw; max-height:100vh; cursor:default; user-select:none; box-shadow:0 12px 40px rgba(0,0,0,0.6); outline:none; }
              </style>
            </head>
            <body>
              <img id="awt" tabindex="0" alt="Java AWT Desktop Window" />
              <script>
                const img = document.getElementById('awt');
                let busy = false;
                function refreshFrame() {
                  if (busy) return;
                  busy = true;
                  const loader = new Image();
                  loader.onload = () => { img.src = loader.src; busy = false; };
                  loader.onerror = () => { busy = false; };
                  loader.src = '/frame.png?t=' + Date.now();
                }
                function coords(ev) {
                  const r = img.getBoundingClientRect();
                  const sx = (img.naturalWidth || r.width) / r.width;
                  const sy = (img.naturalHeight || r.height) / r.height;
                  return { x: Math.round((ev.clientX - r.left) * sx), y: Math.round((ev.clientY - r.top) * sy) };
                }
                img.addEventListener('click', async (ev) => {
                  img.focus();
                  const p = coords(ev);
                  await fetch('/click?x=' + p.x + '&y=' + p.y, { method: 'POST' });
                  setTimeout(refreshFrame, 80);
                });
                img.addEventListener('wheel', async (ev) => {
                  ev.preventDefault();
                  const p = coords(ev);
                  await fetch('/wheel?x=' + p.x + '&y=' + p.y + '&dy=' + Math.round(ev.deltaY), { method: 'POST' });
                  setTimeout(refreshFrame, 80);
                }, { passive: false });
                window.addEventListener('keydown', async (ev) => {
                  if (ev.ctrlKey || ev.metaKey || ev.altKey) return;
                  if (ev.key === 'Shift' || ev.key === 'CapsLock') return;
                  if (ev.key === 'Tab' || ev.key === 'Backspace' || ev.key === 'Enter' || ev.key.startsWith('Arrow')) ev.preventDefault();
                  await fetch('/key', { method: 'POST', body: 'key=' + encodeURIComponent(ev.key) });
                  setTimeout(refreshFrame, 60);
                });
                refreshFrame();
                setInterval(refreshFrame, 350);
              </script>
            </body>
            </html>
            """;
}
