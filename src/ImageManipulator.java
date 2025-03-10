import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.util.Scanner;
import javax.imageio.ImageIO;

public class ImageManipulator {

    public static void main(String[] args) throws IOException {
        char manipulation_type = validateArguments(args);

        Scanner scanner = new Scanner(System.in);
        int[][] pixel_array = new int[0][0];

        switch (manipulation_type) {
            case '1' -> System.out.println("You have provided invalid arguments. Stopping.");
            case 's' -> pixelSort(args);
            case 'g' -> greyscale(args);
            case 'b' -> blur(args);
            case 'm' -> mask(args);
            case 'e' -> edgeDetection(args);
            default -> {
                File image_file = getImageFile(scanner);
                WritableRaster edited_image;

                String manipulation = getManipulationType(scanner);
                BufferedImage img = ImageIO.read(image_file);
                String file_name;

                switch (manipulation) {
                    case "1" -> pixel_array = sortPixels(img, scanner);
                    case "2" -> pixel_array = makeGreyscale(img, scanner);
                    case "3" -> pixel_array = performBlur(img, scanner);
                    case "4" -> pixel_array = createMask(img, scanner);
                    case "5" -> pixel_array = detectEdges(img, scanner);
                    default -> {
                    }
                }

                edited_image = createImage(pixel_array, img);

                file_name = getString(scanner, "What do you wish to save your file as? (do not include filetype)");
                saveImage(file_name, edited_image);
            }
        }
    }

    // Pixel sorting methods
    // #region

    private static void pixelSort(String[] args) throws IOException{
        BufferedImage img = ImageIO.read(new File(args[0]));
        int[][] pixels = get2DPixelArray(img);
        int height = pixels.length, width = pixels[0].length;
        char sorting_property = removeHyphenBeforeArg(args[2]);
        char sort_order = removeHyphenBeforeArg(args[3]);
        boolean sort_vertical = (removeHyphenBeforeArg(args[4]) == 'v' ? true : false);
        boolean use_mask = (removeHyphenBeforeArg(args[5]) == 'm' ? true : false);
        boolean use_edge_detection = (use_mask && removeHyphenBeforeArg(args[6]) == 'e' ? true : false);
        char edge_detection_type = (use_edge_detection ? removeHyphenBeforeArg(args[7]) : '0');
        char masking_property = (!use_edge_detection && use_mask ? removeHyphenBeforeArg(args[7]) : '0');
        int threshold = (use_mask && !use_edge_detection ? Integer.parseInt(args[8]) : 0);

        if(use_mask){
            int[][] mask_of_pixels = new int[height][width];

            if(use_edge_detection){
                if(edge_detection_type == 's'){
                    mask_of_pixels = sobelEdgeDetection(get2DPixelArray(img));
                }
            } else{
                mask_of_pixels = maskArray(pixels, mask_of_pixels, masking_property, threshold);
            }

            pixels = sortPixelWithMask(pixels, mask_of_pixels, height, width, sorting_property, sort_order, sort_vertical);
        } else{
            pixels = sortPixelWihtoutMask(pixels, height, width, sorting_property, sort_order, sort_vertical);
        }

        saveImage(args[args.length - 1], createImage(pixels, img));
    }

    private static int[][] sortPixels(BufferedImage img, Scanner scanner) {
        int[][] pixels = get2DPixelArray(img);

        int height = img.getHeight(), width = img.getWidth();

        boolean use_mask;
        char sorting_property = getCharLimited(scanner,
                "What colour should the pixels be sorted by? (r)ed, (g)reen, (b)lue, (l)uminance, (h)ue",
                new char[] { 'r', 'g', 'b', 'l', 'h' });
        char sort_order = getCharLimited(scanner, "Sort in (a)scending or (d)escending?", new char[] { 'a', 'd' });
        boolean sort_vertical;

        sort_vertical = getSortDirection(scanner);

        use_mask = useMask(scanner);

        if (use_mask) {
            int[][] mask_of_pixels = new int[pixels.length][pixels[0].length];

            boolean use_edge_detection = getBoolean(scanner,
                    "Would you like to use an edge detection algorithm to generate a mask? (y/n)",
                    new String[] { "y", "n" });

            if (use_edge_detection) {
                mask_of_pixels = detectEdges(img, scanner);
            } else {
                char masking_property = getCharLimited(scanner,
                        "What property should the masking be based on? (r)ed, (g)reen, (b)lue, (l)uminance, (h)ue",
                        new char[] { 'r', 'g', 'b', 'l', 'h' });
                mask_of_pixels = createMask2DArray(pixels, mask_of_pixels, masking_property);
            }

            pixels = sortPixelWithMask(pixels, mask_of_pixels, height, width, sorting_property, sort_order,
                    sort_vertical);
        } else {
            pixels = sortPixelWihtoutMask(pixels, height, width, sorting_property, sort_order, sort_vertical);
        }

        return pixels;
    }

    private static int[][] sortPixelWihtoutMask(int[][] pixels, int height, int width, char sorting_property,
            char sort_order, boolean sort_vertical) {
        int[] temp_array;

        if (sort_vertical) {
            for (int col = 0; col < width; col++) {
                temp_array = new int[height];

                for (int row = 0; row < height; row++) {
                    temp_array[row] = pixels[row][col];
                }

                temp_array = mergeSortPixels(temp_array, sorting_property, sort_order);

                for (int row = 0; row < height; row++) {
                    pixels[row][col] = temp_array[row];
                }
            }
        } else {
            for (int row = 0; row < height; row++) {
                temp_array = pixels[row];

                temp_array = mergeSortPixels(temp_array, sorting_property, sort_order);

                System.arraycopy(temp_array, 0, pixels[row], 0, width);
            }
        }

        return pixels;
    }

    private static int[][] sortPixelWithMask(int[][] pixels, int[][] mask, int height, int width, char sorting_property,
            char sort_order, boolean sort_vertical) {
        int[] temp_array;
        int starting_pos;
        int length;
        final int WHITE = 0xffffffff;
        final int BLACK = 0xff000000;

        if (sort_vertical) {
            for (int col = 0; col < width; col++) {
                starting_pos = -1;
                length = 0;

                for (int row = 0; row < height; row++) {
                    if (mask[row][col] == WHITE && starting_pos == -1) {
                        starting_pos = row;
                        length++;
                    } else if (mask[row][col] == WHITE) {
                        length++;
                    } else if (mask[row][col] == BLACK && starting_pos != -1) {
                        temp_array = new int[length];

                        for (int i = 0; i < length; i++) {
                            temp_array[i] = pixels[starting_pos + i][col];
                        }

                        temp_array = mergeSortPixels(temp_array, sorting_property, sort_order);

                        for (int i = 0; i < length; i++) {
                            pixels[starting_pos + i][col] = temp_array[i];
                        }

                        starting_pos = -1;
                        length = 0;
                    }
                }
            }
        } else {
            for (int row = 0; row < height; row++) {
                starting_pos = -1;
                length = 0;

                for (int col = 0; col < width; col++) {
                    if (mask[row][col] == WHITE && starting_pos == -1) {
                        starting_pos = col;
                        length++;
                    } else if (mask[row][col] == WHITE) {
                        length++;
                    } else if (mask[row][col] == BLACK && starting_pos != -1) {
                        temp_array = new int[length];

                        for (int i = 0; i < length; i++) {
                            temp_array[i] = pixels[row][starting_pos + i];
                        }

                        temp_array = mergeSortPixels(temp_array, sorting_property, sort_order);

                        for (int i = 0; i < length; i++) {
                            pixels[row][starting_pos + i] = temp_array[i];
                        }

                        starting_pos = -1;
                        length = 0;
                    }
                }
            }
        }

        return pixels;
    }

    // returns true if vertical, false if horizontal
    private static boolean getSortDirection(Scanner scanner) {
        String input = "";
        boolean valid_input = false;

        while (!valid_input) {
            input = getString(scanner, "What direction do you wish to sort pixels in, (h)orizontal or (v)ertical?");
            input = input.toLowerCase();

            if (input.equals("h") || input.equals("v")) {
                valid_input = true;
            } else {
                System.out.println("You must enter either \"h\" or \"v\".");
            }
        }

        // returns false if input is h or true if not h (which means its v)
        return !input.equals("h");
    }

    private static int[] mergeSortPixels(int[] pixels, char colour_to_sort_by, char sort_order) {
        if (pixels.length == 1) {
            return pixels;
        }

        int mid = pixels.length / 2;
        int[] left = new int[mid];
        int[] right = new int[pixels.length - mid];

        for (int i = 0; i < mid; i++) {
            left[i] = pixels[i];
        }

        for (int i = mid; i < pixels.length; i++) {
            right[i - mid] = pixels[i];
        }

        left = mergeSortPixels(left, colour_to_sort_by, sort_order);
        right = mergeSortPixels(right, colour_to_sort_by, sort_order);

        pixels = mergePixelArrays(left, right, colour_to_sort_by, sort_order);

        return pixels;
    }

    // change this so that instead of taking in a mask it takes in the colour and
    // then write another method that does comparison so we can compare colours +
    // brightness + luminance
    private static int[] mergePixelArrays(int[] left, int[] right, char mask_type, char sort_order) {
        int left_pointer = 0, right_pointer = 0, result_pointer = 0;
        int[] result = new int[left.length + right.length];

        while (left_pointer < left.length && right_pointer < right.length) {
            if (comparePixelProperty((sort_order == 'a' ? 1 : -1) * left[left_pointer],
                    (sort_order == 'a' ? 1 : -1) * right[right_pointer], mask_type)) {
                result[result_pointer] = right[right_pointer];
                result_pointer++;
                right_pointer++;
            } else {
                result[result_pointer] = left[left_pointer];
                result_pointer++;
                left_pointer++;
            }
        }

        while (left_pointer < left.length) {
            result[result_pointer] = left[left_pointer];
            result_pointer++;
            left_pointer++;
        }

        while (right_pointer < right.length) {
            result[result_pointer] = right[right_pointer];
            result_pointer++;
            right_pointer++;
        }

        return result;
    }

    // #endregion

    // Greyscale methods
    // #region

    private static void greyscale(String[] args) throws IOException{
        boolean use_luminance = (removeHyphenBeforeArg(args[2]) == 'l' ? true : false);
        BufferedImage img = ImageIO.read(new File(args[0]));
        int[][] pixels = get2DPixelArray(img);

        int grey;
        int greyscale_colour;

        for (int[] pixel_row : pixels) {
            for (int col = 0; col < pixel_row.length; col++) {
                if (use_luminance) {
                    grey = calculateLuminance(pixel_row[col]);
                } else {
                    grey = calculateBrightness(pixel_row[col]);
                }

                greyscale_colour = (grey << 16) + (grey << 8) + grey;
                pixel_row[col] = greyscale_colour;
            }
        }

        saveImage(args[args.length - 1], createImage(pixels, img));
    }

    private static int[][] makeGreyscale(BufferedImage img, Scanner scanner) {
        boolean use_luminance = getBoolean(scanner,"Do you want to make the image greyscale by (l)uminance or (b)rightness", new String[] { "l", "b" });
        int[][] pixels = get2DPixelArray(img);
        int grey;
        int greyscale_colour;

        for (int[] pixel_row : pixels) {
            for (int col = 0; col < pixel_row.length; col++) {
                if (use_luminance) {
                    grey = calculateLuminance(pixel_row[col]);
                } else {
                    grey = calculateBrightness(pixel_row[col]);
                }

                greyscale_colour = (grey << 16) + (grey << 8) + grey;
                pixel_row[col] = greyscale_colour;
            }
        }

        return pixels;
    }

    // #endregion

    // Blur methods
    // #region

    private static void blur(String[] args) throws IOException{
        BufferedImage img = ImageIO.read(new File(args[0]));
        int[][] pixels = get2DPixelArray(img);
        int size = Integer.parseInt(args[3]);
        char blur_type = removeHyphenBeforeArg(args[2]);

        if(blur_type == 'b'){
            pixels = boxBlur(img, size);
        } else if (blur_type == 'g'){
            pixels = gaussianBlur(img, size);
        }
        
        saveImage(args[args.length - 1], createImage(pixels, img));
    }

    private static int[][] performBlur(BufferedImage img, Scanner scanner) {
        char blur_type = getCharLimited(scanner, "Choose the type of blur to perform: (b)ox blur, (g)aussian blur",
                new char[] { 'b', 'g' });
        int[][] pixels = new int[0][0];
        int size = 0;

        if (blur_type == 'b') {
            pixels = boxBlur(img, getInt(scanner, "Enter a size for the box blur:"));
        } else if (blur_type == 'g') {
            while (size < 1 || size > 25) {
                size = getInt(scanner, "Enter a size for the gaussian blur (max 15):");

                if (size >= 1 && size <= 15) {
                    pixels = gaussianBlur(img, size);
                }
            }

        } else {
            pixels = get2DPixelArray(img);
            System.out.println("Failed to blur image because an invalid blur type was given (somehow).");
        }

        return pixels;
    }

    private static int[][] boxBlur(BufferedImage img, int box_size) {
        int[][] pixels = get2DPixelArray(img);
        int height = img.getHeight();
        int width = img.getWidth();

        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                pixels[row][col] = calculateBoxBlur(pixels, row, col, box_size);
            }
        }

        return pixels;
    }

    private static int[][] gaussianBlur(BufferedImage img, int size) {
        final long[][] GAUSSIAN_DISTRIBUTION = calculateGaussianDistribution(size);
        int[][] pixels = get2DPixelArray(img);
        final int width = img.getWidth(), height = img.getHeight();

        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                pixels[row][col] = calculateGaussianBlur(pixels, row, col, GAUSSIAN_DISTRIBUTION);
            }
        }

        return pixels;
    }

    private static int calculateGaussianBlur(int[][] pixels, int row, int col, long[][] distribution) {
        final int size = (distribution.length - 1) / 2;
        final int mid_point = size;
        final int width = pixels[0].length, height = pixels.length;
        long red = 0, green = 0, blue = 0;
        int pixel_colour;
        int total_distribution_weight = 0;

        for (int x = -size; x <= size; x++) {
            for (int y = -size; y <= size; y++) {
                if (row + x >= 0 && row + x < height && col + y >= 0 && col + y < width) {
                    red += (pixels[row + x][col + y] & 0x000000ff) * distribution[mid_point + y][mid_point + x];
                    green += ((pixels[row + x][col + y] & 0x0000ff00) >>> 8)
                            * distribution[mid_point + y][mid_point + x];
                    blue += ((pixels[row + x][col + y] & 0x00ff0000) >>> 16)
                            * distribution[mid_point + y][mid_point + x];
                    total_distribution_weight += distribution[mid_point + y][mid_point + x];
                }
            }
        }

        red /= total_distribution_weight;
        green /= total_distribution_weight;
        blue /= total_distribution_weight;

        red &= 0xff;
        green &= 0xff;
        blue &= 0xff;

        pixel_colour = (((int) blue) << 16) + (((int) green) << 8) + ((int) red);

        return pixel_colour;
    }

    private static long[][] calculateGaussianDistribution(int size) {
        long[][] distrib = new long[size * 2 + 1][size * 2 + 1];
        final int mid_point = size;

        for (int y = -size; y <= size; y++) {
            if (mid_point + y - 1 < 0) {
                distrib[mid_point + y][0] = 1;
            } else if (mid_point + y > mid_point) {
                distrib[mid_point + y][0] = distrib[mid_point + y - 1][0] / 2;
            } else {
                distrib[mid_point + y][0] = distrib[mid_point + y - 1][0] * 2;
            }

            for (int x = -size + 1; x <= size; x++) {
                if (mid_point + x > mid_point) {
                    distrib[mid_point + y][mid_point + x] = distrib[mid_point + y][mid_point + x - 1] / 2;
                } else {
                    distrib[mid_point + y][mid_point + x] = distrib[mid_point + y][mid_point + x - 1] * 2;
                }
            }
        }

        return distrib;
    }

    private static int calculateBoxBlur(int[][] pixels, int row, int col, int box_size) {
        int red = 0, green = 0, blue = 0, average, pixel_count = 0;

        for (int y = -box_size; y <= box_size; y++) {
            for (int x = -box_size; x <= box_size; x++) {
                if (!(row + y < 0 || row + y >= pixels.length || col + x < 0 || col + x >= pixels[0].length)) {
                    red += pixels[row + y][col + x] & 0x000000ff;
                    green += (pixels[row + y][col + x] & 0x0000ff00) >>> 8;
                    blue += (pixels[row + y][col + x] & 0x00ff0000) >>> 16;
                    pixel_count++;
                }
            }
        }

        red /= pixel_count;
        green /= pixel_count;
        blue /= pixel_count;

        average = (blue << 16) + (green << 8) + red;

        return average;
    }

    // #endregion

    // Masking methods
    // #region

    private static void mask(String[] args) throws IOException{
        BufferedImage img = ImageIO.read(new File(args[0]));
        int[][] pixels = get2DPixelArray(img);
        boolean use_edge_detection = (removeHyphenBeforeArg(args[2]) == 'e' ? true : false);
        char property_to_mask = removeHyphenBeforeArg(args[3]);
        int threshold = (!use_edge_detection ? Integer.parseInt(args[4]) : 0);

        if(use_edge_detection){
            pixels = sobelEdgeDetection(pixels);
        } else{
            pixels = maskArray(pixels, pixels, property_to_mask, threshold);
        }

        saveImage(args[args.length - 1], createImage(pixels, img));
    }

    private static int[][] createMask2DArray(int[][] pixels, int[][] array_to_write_to, char property_to_mask_with) {
        final int WHITE = 0xffffffff; // left most 0xff000000 is so that the image is not transparent
        final int BLACK = 0xff000000;

        final int THRESHOLD = (useCustomThreshold(new Scanner(System.in)) ? getCustomThreshold(new Scanner(System.in))
                : otsuThreshold(pixels, property_to_mask_with));

        for (int row = 0; row < pixels.length; row++) {
            for (int col = 0; col < pixels[0].length; col++) {
                int value = getPixelProperty(pixels[row][col], property_to_mask_with);

                if (value >= (THRESHOLD & 0xff)) {
                    array_to_write_to[row][col] = WHITE;
                } else {
                    array_to_write_to[row][col] = BLACK;
                }
            }
        }

        return array_to_write_to;
    }

    private static int[][] maskArray(int[][] pixels, int[][] array_to_write_to, char property_to_mask_with, int THRESHOLD){
        final int WHITE = 0xffffffff; // left most 0xff000000 is so that the image is not transparent
        final int BLACK = 0xff000000;

        if(THRESHOLD == -1){
            THRESHOLD = otsuThreshold(pixels, property_to_mask_with);
        }

        for (int row = 0; row < pixels.length; row++) {
            for (int col = 0; col < pixels[0].length; col++) {
                int value = getPixelProperty(pixels[row][col], property_to_mask_with);

                if (value >= (THRESHOLD & 0xff)) {
                    array_to_write_to[row][col] = WHITE;
                } else {
                    array_to_write_to[row][col] = BLACK;
                }
            }
        }

        return array_to_write_to;   
    }

    private static boolean useCustomThreshold(Scanner scanner) {
        return getBoolean(scanner, "Would you like to use a custom threshold for masking? (y/n)",
                new String[] { "y", "n" });
    }

    private static int getCustomThreshold(Scanner scanner) {
        int threshold = 0;
        boolean white_below_threshold;
        boolean is_valid = false;

        while (!is_valid) {
            threshold = getInt(scanner, "Enter the value for the threshold (>=0 and <256)");

            if (threshold >= 0 && threshold < 256) {
                is_valid = true;
            } else {
                System.out.println("Threshold not in range. It must be >=0 and <256");
            }
        }

        white_below_threshold = getBoolean(scanner,
                "Should white be used for values that are (a)bove or equal to the threshold, or values (b)elow it? (a/b)",
                new String[] { "b", "a" });

        if (white_below_threshold) {
            threshold += 0x100;
        }

        return threshold;
    }

    private static boolean useMask(Scanner scanner) {
        String response = "";
        boolean valid = false;

        while (!valid) {
            response = getString(scanner, "Should a mask be used? (y/n)");
            response = response.toLowerCase();

            if (response.equals("y") || response.equals("n")) {
                valid = true;
            } else {
                System.out.println("You must enter either \"y\" or \"n\".");
            }
        }

        return response.equals("y");
    }

    private static int otsuThreshold(int[][] pixels, char property) {
        final int range = 256;
        int[] histogram = new int[range];
        float[] square_variance = new float[range];
        float weight_background, weight_foreground, mu_background, mu_foreground;
        float highest_variance = -1;
        int highest_variance_index = 0;

        for (int row = 0; row < pixels.length; row++) {
            for (int col = 0; col < pixels[0].length; col++) {
                int value = getPixelProperty(pixels[row][col], property);
                histogram[value]++;
            }
        }

        for (int i = 0; i < histogram.length; i++) {
            weight_background = 0;
            weight_foreground = 0;
            mu_background = 0;
            mu_foreground = 0;
            int mean_number = 0;

            for (int j = 0; j <= i; j++) {
                weight_background += histogram[i];
                mu_background += i * histogram[i];
                mean_number += histogram[i];
            }

            mu_background /= mean_number;
            weight_background /= range;
            mean_number = 0;

            for (int j = i + 1; j < histogram.length; j++) {
                weight_foreground += histogram[i];
                mu_foreground += i * histogram[i];
                mean_number += histogram[i];
            }

            mu_foreground /= mean_number;
            weight_foreground /= range;

            square_variance[i] = weight_background * weight_foreground * ((mu_background - mu_foreground) * (mu_background - mu_foreground));
        }

        for (int i = 0; i < square_variance.length; i++) {
            if (square_variance[i] > highest_variance) {
                highest_variance = square_variance[i];
                highest_variance_index = i;
            }
        }

        return highest_variance_index;
    }

    private static int[][] createMask(BufferedImage img, Scanner scanner) {
        int[][] pixels = get2DPixelArray(img);
        char property = getCharLimited(scanner,
                "What property should the masking be based on? (r)ed, (g)reen, (b)lue, (l)uminance, (h)ue",
                new char[] { 'r', 'g', 'b', 'l', 'h' });

        pixels = createMask2DArray(pixels, pixels, property);

        return pixels;
    }

    // #endregion

    // Edge detection methods
    // #region

    private static void edgeDetection(String[] args) throws IOException{
        BufferedImage img = ImageIO.read(new File(args[0]));
        int[][] pixels = get2DPixelArray(img);
        char detection_type = removeHyphenBeforeArg(args[2]);

        pixels = switch (detection_type){
            case 's' -> sobelEdgeDetection(pixels);
            default -> pixels;
        };

        saveImage(args[args.length - 1], createImage(pixels, img));
    }

    private static int[][] detectEdges(BufferedImage img, Scanner scanner) {
        int[][] pixels = get2DPixelArray(img);

        char detection_type = getCharLimited(scanner, "Enter the type of edge detection you wish to perform: (s)obel",
                new char[] { 's' });

        pixels = switch (detection_type) {
            case 's' -> sobelEdgeDetection(pixels);
            default -> pixels;
        };

        return pixels;
    }

    private static int[][] sobelEdgeDetection(int[][] pixels) {
        final int WHITE = 0xffffffff;
        final int BLACK = 0xff000000;

        final int[][] horizontal_kernel = new int[][] { { -1, 0, 1 },
                { -2, 0, 2 },
                { -1, 0, 1 } };

        final int[][] vertical_kernel = new int[][] { { 1, 2, 1 },
                { 0, 0, 0 },
                { -1, -2, -1 } };

        int[][] weights = new int[pixels.length][pixels[0].length];
        final int width = pixels[0].length, height = pixels.length;
        int sum = 0;
        int temp_value;
        int threshold;

        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                // temp = sqrt(x * x + y * y);
                temp_value = (int) Math.round(
                        Math.sqrt(Math.pow((double) calculateSobelWeight(vertical_kernel, pixels, row, col), 2.0d)
                                + Math.pow((double) calculateSobelWeight(horizontal_kernel, pixels, row, col), 2.0d)));

                weights[row][col] += temp_value;
                sum += temp_value;
            }
        }

        threshold = sum / (width * height);

        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                if (weights[row][col] < threshold) {
                    pixels[row][col] = WHITE;
                } else {
                    pixels[row][col] = BLACK;
                }
            }
        }

        return pixels;
    }

    private static int calculateSobelWeight(int[][] kernel, int[][] pixels, int row, int col) {
        int weight = 0;
        final int midpoint = 1;
        final int size = 1;

        for (int y = -size; y <= size; y++) {
            for (int x = -size; x <= size; x++) {
                if (row + y >= 0 && row + y < pixels.length && col + x >= 0 && col + x < pixels[0].length) {
                    weight += calculateLuminance(pixels[row + y][col + x]) * kernel[midpoint + y][midpoint + x];
                }
            }
        }

        return weight;
    }

    /*
     * private static int[][] cannyEdgeDetection(int[][] pixels){
     * // 1. gaussian blur //might have to rework some methods again :))))
     * // 2. find intensity (via some kind of operator (sobel bc already done it))
     * // 3. apply threshold to get rid of false edges
     * // 4. apply double threshold (???) to determine potential edges
     * // 5. track edge by hysteresis
     * // 5.1. done by blob analysis (look at 8 neighbours)
     * }
     */

    // #endregion

    // GENERIC METHODS
    // #region

    private static char removeHyphenBeforeArg(String arg){
        return arg.split("-")[1].charAt(0);
    }

    // returns '0' if the arguments are not given, '1' if the arguments are not
    // valid, 's' if the arguments are for sorting, 'g' if the arguments are for
    // making greyscale,
    // 'b' if the arguments are for blurring, 'm' if the arguments are for masking
    // or 'e' if the arguments are for edge detection.
    private static char validateArguments(String[] args) {
        if (args.length == 0) {
            return '0';
        }

        String colour_pattern = "-(r|red|g|green|b|blue|l|luminance|h|hue)";

        if (!new File(args[0]).exists()) {
            System.out.println(args[0] + " is not a valid file path.");
            return '1';
        }

        if (args[1].matches("-(s|sort)")) {
            if (!args[2].matches(colour_pattern)) {
                System.out.println(args[2] + " does not match the regex: " + colour_pattern);
                return '1';
            }

            if(!args[3].matches("-(a|ascending|d|descending)")) {
                System.out.println(args[3] + " does not match the regex: -(a|ascending|d|descending)");
                return '1';
            }

            if(!args[4].matches("-(v|vertical|h|horizontal)")){
                System.out.println(args[4] + " does not match the regex: -(v|vertical|h|horizontal)");
                return '1';
            }

            if (args[5].matches("-(m|mask)")) {
                if (args[6].matches("-(n|no-edge)")) {
                    if (!args[7].matches(colour_pattern)) {
                        System.out.println(args[7] + " does not match the regex: " + colour_pattern);
                        return '1';
                    }

                    if(!checkIfInt(args[8])){
                        System.out.println(args[8] + " is not an integer. If you want to set a custom threshold give a number >=0 and <256. If you want one calculated via Otsu's method, give -1.");
                        return '1';
                    }
                } else if(args[6].matches("-(e|edge)")){
                    if(!args[7].matches("-(s|sobel)")){
                        System.out.println(args[7] + " does not match the regex: -(s|sobel)");
                        return '1';
                    }
                    
                } else{
                    System.out.println(args[6] + " does not match the regex: -(e|edge|n|no-edge)");
                    return '1';
                }
            } else if (!args[5].matches("-(n|no-mask)")) {
                System.out.println(args[5] + " does not match the regex: -(m|mask|n|no-mask)");
                return '1';
            }

            return 's';
        }

        if (args[1].matches("-(g|greyscale|grayscale)")) {
            if (!args[2].matches("-(l|luminance|b|brightness)")) {
                System.out.println(args[2] + " does not match the regex: -(l|luminance|b|brightness)");
                return '1';
            }

            return 'g';
        }

        if (args[1].matches("-(m|mask)")) {
            if (args[2].matches("-(e|edge|n|no-edge)")) {
                if (!args[3].matches(colour_pattern)) {
                    System.out.println(args[3] + " does not match the regex: " + colour_pattern);
                    return '1';
                } else if(!checkIfInt(args[4])){
                    System.out.println(args[4] + " is not an integer. If you want to set a custom threshold give a number >=0 and <256. If you want one calculated via Otsu's method, give -1.");
                    return '1';
                }
            } else {
                System.out.println(args[2] + " does not match the regex: -(e|edge|n|no-edge)");
                return '1';
            }

            return 'm';
        }

        if (args[1].matches("-(e|edge|edge-detection)")) {
            if (!args[2].matches("-(s|sobel)")) {
                System.out.println(args[2] + " does not match the regex: -(s|sobel)");
                return '1';
            }

            return 'e';
        }

        if (args[1].matches("-(b|blur)")) {
            if (!args[2].matches("-(b|box|g|gaussian)")) {
                System.out.println(args[2] + " does not match the regex: -(b|box|g|gaussian)");
                return '1';
            } else{
                if(!checkIfInt(args[3])){
                    System.out.println(args[3] + " is not an integer.");
                    return '1';
                } else{
                    int size = Integer.parseInt(args[3]);

                    if(size <= 0){
                        System.out.println(args[3] + " must be greater than 0.");
                        return '1';
                    } else if (size > 15 && (args[2].equals("-g") ||args[2].equals("-gaussian"))){
                        System.out.println("The kernel size limit for gaussian blur is 15. Please enter a number >0 and <=15");
                        return '1';
                    }
                }
            }

            return 'b';
        }

        System.out.println(args[1] + " does not match the regex: -(m|mask|s|sort|g|grayscale|e|edge|edge-detection|b|blur)");
        return '1';
    }

    private static char getCharLimited(Scanner scanner, String message, char[] valid_chars) {
        char response = ' ';

        while (!arrayContainsChar(valid_chars, response)) {
            response = getChar(scanner, message);

            if (!arrayContainsChar(valid_chars, response)) {
                System.out.println("You must enter one of the characters in the brackets.");
            }
        }

        return response;
    }

    private static char getChar(Scanner scanner, String message) {
        String response = "";
        boolean valid_response = false;

        while (!valid_response) {
            response = getString(scanner, message);

            if (response.length() == 1) {
                valid_response = true;
            } else {
                System.out.println("You must enter a single character.");
            }
        }

        return response.charAt(0);
    }

    private static int getPixelProperty(int pixel, char property) {
        return switch (property) {
            case 'r' -> pixel & 0x000000ff;
            case 'g' -> (pixel & 0x0000ff00) >>> 8;
            case 'b' -> (pixel & 0x00ff0000) >>> 16;
            case 'l' -> calculateLuminance(pixel);
            case 'h' -> calculateHue(pixel);
            default -> pixel;
        };
    }

    private static int[][] get2DPixelArray(BufferedImage img) {
        Raster image_raster = img.getData();
        int width = image_raster.getWidth(), height = image_raster.getHeight();

        int[][] pixels = new int[height][width];
        int values_per_pixel = (img.getAlphaRaster() != null) ? 4 /* rgba */ : 3 /* rgb */;
        int[] temp = new int[values_per_pixel];

        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                temp = image_raster.getPixel(col, row, temp);
                // returns rgba

                // abgr
                if (values_per_pixel == 4) {
                    pixels[row][col] = ((temp[3] & 0xff) << 24) + ((temp[2] & 0xff) << 16) + ((temp[1] & 0xff) << 8)
                            + (temp[0] & 0xff);
                } else { // bgr
                    pixels[row][col] = (temp[2] << 16) + (temp[1] << 8) + temp[0];
                }

                pixels[row][col] &= 0xffffffff;
            }
        }

        return pixels;
    }

    private static WritableRaster createImage(int[][] sorted_pixels, BufferedImage img) {
        WritableRaster raster = img.getRaster();
        int width = raster.getWidth(), height = raster.getHeight();

        int values_per_pixel = (img.getAlphaRaster() != null) ? 4 : 3;
        int[] temp = new int[values_per_pixel];

        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                temp[0] = (sorted_pixels[row][col] & 0x000000ff);
                temp[1] = (sorted_pixels[row][col] & 0x0000ff00) >>> 8;
                temp[2] = (sorted_pixels[row][col] & 0x00ff0000) >>> 16;

                if (values_per_pixel == 4) {
                    temp[3] = (sorted_pixels[row][col] & 0xff000000) >>> 24;
                }

                raster.setPixel(col, row, temp);
            }
        }

        return raster;
    }

    private static int calculateBrightness(int abgr) {
        int red, green, blue;
        red = abgr & 0x000000FF;
        green = (abgr & 0x0000FF00) >>> 8;
        blue = (abgr & 0x00FF0000) >>> 16;

        return (red + green + blue) / 3;
    }

    private static int calculateLuminance(int abgr) {
        int red, green, blue;
        int red_coefficient = (int) Math.round(0.2126 * 255);
        int green_coefficient = (int) Math.round(0.7152 * 255);
        int blue_coefficient = (int) Math.round(0.0722 * 255);

        red = abgr & 0x000000FF;
        green = (abgr & 0x0000FF00) >>> 8;
        blue = (abgr & 0x00FF0000) >>> 16;

        return (red * red_coefficient + green * green_coefficient + blue * blue_coefficient) / 255;
    }

    private static void saveImage(String file_name, WritableRaster img) throws IOException {
        File img_file = new File(file_name + ".png");
        int type = img.getNumBands() == 4 ? BufferedImage.TYPE_4BYTE_ABGR : BufferedImage.TYPE_3BYTE_BGR;
        BufferedImage buffer = new BufferedImage(img.getWidth(), img.getHeight(), type);
        buffer.setData(img);
        ImageIO.write(buffer, "png", img_file);
    }

    private static boolean arrayContainsString(String[] array, String target) {
        for (String string_in_array : array) {
            if (string_in_array.equals(target)) {
                return true;
            }
        }

        return false;
    }

    private static String getString(Scanner scanner, String message) {
        System.out.println(message);
        return scanner.nextLine();
    }

    private static File getImageFile(Scanner scanner) {
        String image_path = getString(scanner, "Enter the full path of the image you want to manipulate.");
        File image = new File(image_path);

        while (!isFileValid(image)) {
            System.out.println("File path is invalid, please try again.");
            image_path = getString(scanner, "Enter the full path of the image you want to manipulate.");
            image = new File(image_path);
        }

        return new File(image_path);
    }

    private static int getInt(Scanner scanner, String message) {
        String input;
        int input_as_int = 0;
        boolean is_valid = false;

        while (!is_valid) {
            System.out.println(message);
            input = scanner.nextLine();

            if (checkIfInt(input)) {
                input_as_int = Integer.parseInt(input);
                is_valid = true;
            } else {
                System.out.println("You must enter an integer.");
            }
        }

        return input_as_int;
    }

    /**
     * 
     * @param scanner
     * @param message
     * @param valid_responses // the first element must be the true one and there
     *                        must be at least 2 elements
     * @return boolean
     */
    private static boolean getBoolean(Scanner scanner, String message, String[] valid_responses) {
        String input;
        boolean return_value = false;
        boolean is_valid = false;

        while (!is_valid) {
            input = getString(scanner, message);

            if (input.equals(valid_responses[0])) {
                return_value = true;
                is_valid = true;
            } else {
                for (int i = 1; i < valid_responses.length; i++) {
                    if (input.equals(valid_responses[i])) {
                        return_value = false;
                        is_valid = true;
                    }
                }
            }

            if (!is_valid) {
                System.out.println("That is not a valid response. You must type one of the following:");
                for (String response : valid_responses) {
                    System.out.println("\"" + response + "\"");
                }
            }
        }

        return return_value;
    }

    private static boolean checkIfInt(String s) {
        boolean valid_char;

        for (int i = 0; i < s.length(); i++) {
            valid_char = false;
            if ('0' <= s.charAt(i) && s.charAt(i) <= '9') {
                valid_char = true;
            } else if (i == 0 && s.charAt(i) == '-') {
                valid_char = true;
            }

            if (!valid_char) {
                return false;
            }
        }

        return true;
    }

    private static boolean isFileValid(File file) {
        return file.exists();
    }

    private static int calculateHue(int abgr) {
        double red, green, blue;
        double hue = 0;

        red = (double) (abgr & 0x000000ff) / 256.0d;
        green = (double) ((abgr & 0x0000ff00) >>> 8) / 256.0d;
        blue = (double) ((abgr & 0x00ff0000) >>> 16) / 256.0d;

        hue = Math.atan2(Math.sqrt(3) * (green - blue), 2 * red - green - blue);

        if(hue < 0){
            hue += 360;
        } else if (hue > 360){
            hue -= 360;
        }

        return (int) (hue * (17.0d / 24.0d)); // scales hue from 0-360 to 0-255
    }

    // returns true if the left pixel's property is greater than the right pixel's
    private static boolean comparePixelProperty(int left_pixel, int right_pixel, char property) {
        int mask;

        mask = switch (property) {
            case 'b' -> 0x00ff0000;
            case 'g' -> 0x0000ff00;
            case 'r' -> 0x000000ff;
            default -> 0xffffffff; // this is for grey_scale and overall comparison
        };

        return switch (property) {
            case 'l' -> calculateLuminance(left_pixel) > calculateLuminance(right_pixel);
            case 'h' -> calculateHue(left_pixel) > calculateHue(right_pixel);
            default -> (left_pixel & mask) > (right_pixel & mask);
        };

    }

    private static String getManipulationType(Scanner scanner) {
        String[] valid_choices = { "1", "2", "3", "4", "5" };
        String choice = "";
        boolean choice_is_valid = false;

        while (!choice_is_valid) {
            choice = getString(scanner,
                    "What type of image manipulation do you want to perform?\n(1) Pixel Sorting   (2) Make Greyscale   (3) Blur   (4) Create Image Mask   (5) Edge Detection");

            if (arrayContainsString(valid_choices, choice)) {
                choice_is_valid = true;
            } else {
                System.out.println("You must enter the number on the left of each option");
            }
        }

        return choice;
    }

    private static boolean arrayContainsChar(char[] array, char target) {
        for (int i = 0; i < array.length; i++) {
            if (array[i] == target) {
                return true;
            }
        }

        return false;
    }

    // #endregion
}
