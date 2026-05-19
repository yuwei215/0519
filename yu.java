import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import javax.swing.*;

public class yu extends JFrame {

    BufferedImage original, result;
    JLabel originalLabel, resultLabel;

    JSlider edgeSlider, colorSlider, brightSlider, blurSlider;

    public yu() {
        setTitle("Cartoon Filter Adjustable Version");
        setSize(1100, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        originalLabel = new JLabel("原圖", SwingConstants.CENTER);
        resultLabel = new JLabel("結果", SwingConstants.CENTER);

        JPanel imagePanel = new JPanel(new GridLayout(1, 2, 10, 10));
        imagePanel.add(originalLabel);
        imagePanel.add(resultLabel);

        edgeSlider = createSlider(50, 300, 210);
        colorSlider = createSlider(20, 120, 75);
        brightSlider = createSlider(0, 80, 35);
        blurSlider = createSlider(0, 5, 2);

        JPanel controlPanel = new JPanel(new GridLayout(4, 1));
        controlPanel.add(makeControl("邊緣強度", edgeSlider));
        controlPanel.add(makeControl("色彩分層", colorSlider));
        controlPanel.add(makeControl("亮度", brightSlider));
        controlPanel.add(makeControl("模糊次數", blurSlider));

        JButton openBtn = new JButton("選擇圖片");
        JButton runBtn = new JButton("產生卡通化");
        JButton saveBtn = new JButton("儲存圖片");

        JPanel btnPanel = new JPanel();
        btnPanel.add(openBtn);
        btnPanel.add(runBtn);
        btnPanel.add(saveBtn);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(controlPanel, BorderLayout.CENTER);
        bottomPanel.add(btnPanel, BorderLayout.SOUTH);

        add(imagePanel, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        openBtn.addActionListener(e -> openImage());
        runBtn.addActionListener(e -> processImage());
        saveBtn.addActionListener(e -> saveImage());

        edgeSlider.addChangeListener(e -> processImage());
        colorSlider.addChangeListener(e -> processImage());
        brightSlider.addChangeListener(e -> processImage());
        blurSlider.addChangeListener(e -> processImage());
    }

    JSlider createSlider(int min, int max, int value) {
        JSlider slider = new JSlider(min, max, value);
        slider.setPaintTicks(true);
        slider.setPaintLabels(true);
        slider.setMajorTickSpacing((max - min) / 4);
        return slider;
    }

    JPanel makeControl(String name, JSlider slider) {
        JPanel panel = new JPanel(new BorderLayout());
        JLabel label = new JLabel(name + "：" + slider.getValue());
        slider.addChangeListener(e -> label.setText(name + "：" + slider.getValue()));
        panel.add(label, BorderLayout.WEST);
        panel.add(slider, BorderLayout.CENTER);
        return panel;
    }

    void openImage() {
        JFileChooser chooser = new JFileChooser();
        int r = chooser.showOpenDialog(this);

        if (r == JFileChooser.APPROVE_OPTION) {
            try {
                original = ImageIO.read(chooser.getSelectedFile());
                originalLabel.setIcon(new ImageIcon(resize(original, 500, 450)));
                originalLabel.setText("");
                processImage();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "讀取圖片失敗");
            }
        }
    }

    void processImage() {
        if (original == null) return;

        int edgeValue = edgeSlider.getValue();
        int colorValue = colorSlider.getValue();
        int brightValue = brightSlider.getValue();
        int blurTimes = blurSlider.getValue();

        BufferedImage smooth = blur(original, blurTimes);
        int[][] gray = toGray(smooth);
        int[][] edge = sobel(gray, edgeValue);

        int w = smooth.getWidth();
        int h = smooth.getHeight();

        result = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {

                Color c = new Color(smooth.getRGB(x, y));

                int red = cartoonColor(c.getRed(), colorValue);
                int green = cartoonColor(c.getGreen(), colorValue);
                int blue = cartoonColor(c.getBlue(), colorValue);

                red = brighten(red, brightValue);
                green = brighten(green, brightValue);
                blue = brighten(blue, brightValue);

                if (red > 210 && green > 210 && blue > 210) {
                    red = 238;
                    green = 235;
                    blue = 225;
                }

                if (edge[x][y] == 0) {
                    result.setRGB(x, y, new Color(45, 45, 45).getRGB());
                } else {
                    result.setRGB(x, y, new Color(red, green, blue).getRGB());
                }
            }
        }

        resultLabel.setIcon(new ImageIcon(resize(result, 500, 450)));
        resultLabel.setText("");
    }

    void saveImage() {
        if (result == null) {
            JOptionPane.showMessageDialog(this, "請先產生圖片");
            return;
        }

        try {
            ImageIO.write(result, "jpg", new File("cartoon_output.jpg"));
            JOptionPane.showMessageDialog(this, "已儲存 cartoon_output.jpg");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "儲存失敗");
        }
    }

    static Image resize(BufferedImage img, int maxW, int maxH) {
        int w = img.getWidth();
        int h = img.getHeight();

        double scale = Math.min((double) maxW / w, (double) maxH / h);

        return img.getScaledInstance(
                (int)(w * scale),
                (int)(h * scale),
                Image.SCALE_SMOOTH
        );
    }

    static BufferedImage blur(BufferedImage img, int times) {
        BufferedImage current = img;

        for (int t = 0; t < times; t++) {
            int w = current.getWidth();
            int h = current.getHeight();

            BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);

            int[][] kernel = {
                    {1, 2, 1},
                    {2, 4, 2},
                    {1, 2, 1}
            };

            for (int y = 1; y < h - 1; y++) {
                for (int x = 1; x < w - 1; x++) {

                    int r = 0, g = 0, b = 0, sum = 0;

                    for (int j = -1; j <= 1; j++) {
                        for (int i = -1; i <= 1; i++) {
                            Color c = new Color(current.getRGB(x + i, y + j));
                            int k = kernel[j + 1][i + 1];

                            r += c.getRed() * k;
                            g += c.getGreen() * k;
                            b += c.getBlue() * k;
                            sum += k;
                        }
                    }

                    out.setRGB(x, y, new Color(r / sum, g / sum, b / sum).getRGB());
                }
            }

            current = out;
        }

        return current;
    }

    static int[][] toGray(BufferedImage img) {
        int w = img.getWidth();
        int h = img.getHeight();

        int[][] gray = new int[w][h];

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                Color c = new Color(img.getRGB(x, y));
                gray[x][y] = (int)(
                        0.299 * c.getRed()
                                + 0.587 * c.getGreen()
                                + 0.114 * c.getBlue()
                );
            }
        }

        return gray;
    }

    static int[][] sobel(int[][] gray, int threshold) {
        int w = gray.length;
        int h = gray[0].length;

        int[][] edge = new int[w][h];

        int[][] gx = {
                {-1, 0, 1},
                {-2, 0, 2},
                {-1, 0, 1}
        };

        int[][] gy = {
                {-1, -2, -1},
                {0, 0, 0},
                {1, 2, 1}
        };

        for (int y = 1; y < h - 1; y++) {
            for (int x = 1; x < w - 1; x++) {

                int sx = 0;
                int sy = 0;

                for (int j = -1; j <= 1; j++) {
                    for (int i = -1; i <= 1; i++) {
                        sx += gray[x + i][y + j] * gx[j + 1][i + 1];
                        sy += gray[x + i][y + j] * gy[j + 1][i + 1];
                    }
                }

                int mag = Math.abs(sx) + Math.abs(sy);

                if (mag > threshold) {
                    edge[x][y] = 0;
                } else {
                    edge[x][y] = 255;
                }
            }
        }

        return edge;
    }

    static int cartoonColor(int value, int level) {
        int q = (value / level) * level;
        return (q + value) / 2;
    }

    static int brighten(int value, int add) {
        value += add;

        if (value > 255) value = 255;
        if (value < 0) value = 0;

        return value;
    }

    public static void main(String[] args) {
        new yu().setVisible(true);
    }
}