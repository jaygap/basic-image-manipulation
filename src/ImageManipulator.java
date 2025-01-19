import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.util.Scanner;
import javax.imageio.ImageIO;


public class ImageManipulator {

    public static void main(String[] args) throws IOException {
        Scanner scanner = new Scanner(System.in);
        File image_file = getImageFile(scanner);
        WritableRaster edited_image;

        String manipulation_type = getManipulationType(scanner);
        BufferedImage img = ImageIO.read(image_file);
        String file_name;
        int[][] pixel_array = new int[0][0];

        switch (manipulation_type) {
            case "1" -> pixel_array = sortPixels(img, scanner);
            case "2" -> pixel_array = makeGreyscale(img, scanner);
            case "3" -> pixel_array = performBlur(img, scanner);
            case "4" -> pixel_array = createMask(img);
            default -> {
            }
        }

        edited_image = createImage(pixel_array, img);

        file_name = getString(scanner, "What do you wish to save your file as? (do not include filetype)");
        saveImage(file_name, edited_image);
    }
    
    private static int[][] sortPixels(BufferedImage img, Scanner scanner){
        int[][] pixels = get2DPixelArray(img);

        int height = img.getHeight(), width = img.getWidth(); 

        boolean use_mask;
        char sorting_property = getColourToSortBy(scanner);
        boolean sort_vertical;

        sort_vertical = getSortDirection(scanner);  

        use_mask = useMask(scanner);


        if(use_mask){
            int[][] mask_of_pixels = new int[pixels.length][pixels[0].length];
            mask_of_pixels = createMask2DArray(pixels, mask_of_pixels);

            pixels = sortPixelWithMask(pixels, mask_of_pixels, height, width, sorting_property, sort_vertical);
        } else{
            pixels = sortPixelWihtoutMask(pixels, height, width, sorting_property, sort_vertical);
        }

        return pixels;
    }

    private static int[][] sortPixelWihtoutMask(int[][] pixels, int height, int width, char sorting_property,boolean sort_vertical){
        int[] temp_array;

        if(sort_vertical){
            for(int col = 0; col < width; col++){
                temp_array = new int[height];
                
                for(int row = 0; row < height; row++){
                    temp_array[row] = pixels[row][col];
                }

                temp_array = mergeSortPixels(temp_array, sorting_property);

                for(int row = 0; row < height; row++){
                    pixels[row][col] = temp_array[row];
                }
            }
        } else{
            for(int row = 0; row < height; row++){
                temp_array = pixels[row];

                temp_array = mergeSortPixels(temp_array, sorting_property);

                System.arraycopy(temp_array, 0, pixels[row], 0, width);
            }
        }

        return pixels;
    }

    private static int[][] sortPixelWithMask(int[][] pixels, int[][] mask, int height, int width, char sorting_property, boolean sort_vertical){
        int[] temp_array;
        int starting_pos;
        int length;
        final int WHITE = 0x00ffffff;
        final int BLACK = 0x00000000;

        if(sort_vertical){
            for(int col = 0; col < width; col++){
                starting_pos = -1;
                length = 0;

                for(int row = 0; row < height; row++){
                    if(mask[row][col] == WHITE && starting_pos == -1){
                        starting_pos = row;
                        length++;
                    } else if(mask[row][col] == WHITE){
                        length++;
                    } else if(mask[row][col] == BLACK && starting_pos != -1){
                        temp_array = new int[length];

                        for(int i = 0; i < length; i++){
                            temp_array[i] = pixels[starting_pos + i][col];
                        }

                        temp_array = mergeSortPixels(temp_array, sorting_property);

                        for(int i = 0; i < length; i++){
                            pixels[starting_pos + i][col] = temp_array[i];
                        }

                        starting_pos = -1;
                        length = 0;
                    }
                }
            }
        } else{
            for(int row = 0; row < height; row++){
                starting_pos = -1;
                length = 0;

                for(int col = 0; col < width; col++){
                    if(mask[row][col] == WHITE && starting_pos == -1){
                        starting_pos = col;
                        length++;
                    } else if(mask[row][col] == WHITE){
                        length++;
                    } else if(mask[row][col] == BLACK && starting_pos != -1){
                        temp_array = new int[length];

                        for(int i = 0; i < length; i++){
                            temp_array[i] = pixels[row][starting_pos + i];
                        }

                        temp_array = mergeSortPixels(temp_array, sorting_property);

                        for(int i = 0; i < length; i++){
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

    private static int[][] createMask2DArray(int[][] pixels, int[][] array_to_write_to){
        final int WHITE = 0x00ffffff;
        final int BLACK = 0x00000000;

        final int THRESHOLD = otsuThreshold(pixels);

        for(int row = 0; row < pixels.length; row++){
            for(int col = 0; col < pixels[0].length; col++){
                int luminance = calculateLuminance(pixels[row][col]);

                if(luminance <= THRESHOLD){
                    array_to_write_to[row][col] = WHITE;
                } else{
                    array_to_write_to[row][col] = BLACK;
                }
            }
        }

        return array_to_write_to;
    }

    private static int otsuThreshold(int[][] pixels){
        final int luminance_range = 256;
        int[] luminance_histogram = new int[luminance_range];
        float[] square_variance = new float[luminance_range];
        float weight_background, weight_foreground, mu_background, mu_foreground; 
        float highest_variance = -1;
        int highest_variance_index = 0;

        for(int row = 0; row < pixels.length; row++){
            for(int col = 0; col < pixels[0].length; col++){
                int luminance = calculateLuminance(pixels[row][col]);
                luminance_histogram[luminance]++;
            }
        }

        for(int i = 0; i < luminance_histogram.length; i++){
            weight_background = 0;
            weight_foreground = 0;
            mu_background = 0;
            mu_foreground = 0;
            int mean_number = 0;

            for(int j = 0; j <= i; j++){
                weight_background += luminance_histogram[i];
                mu_background += i * luminance_histogram[i];
                mean_number += luminance_histogram[i];
            }

            mu_background /= mean_number;
            weight_background /= luminance_range;
            mean_number = 0;

            for(int j = i + 1; j < luminance_histogram.length; j++){
                weight_foreground += luminance_histogram[i];
                mu_foreground += i * luminance_histogram[i];
                mean_number += luminance_histogram[i];
            }

            mu_foreground /= mean_number;
            weight_foreground /= luminance_range;

            square_variance[i] = weight_background * weight_foreground * ((mu_background - mu_foreground) * (mu_background - mu_foreground));
        }

        for(int i = 0; i < square_variance.length; i++){
            if(square_variance[i] > highest_variance){
                highest_variance = square_variance[i];
                highest_variance_index = i;
            }
        }

        return highest_variance_index;
    }

    private static String getManipulationType(Scanner scanner){
        String[] valid_choices = {"1", "2", "3", "4"};
        String choice = "";
        boolean choice_is_valid = false;

        while(!choice_is_valid){
            choice = getString(scanner, "What type of image manipulation do you want to perform?\n(1) Pixel Sorting   (2) Make Greyscale   (3) Blur   (4) Create Image Mask");

            if(arrayContainsString(valid_choices, choice)){
                choice_is_valid = true;
            } else{
                System.out.println("You must enter the number on the left of each option");
            }
        }

        return choice;
    }

    private static int[][] createMask(BufferedImage img){
        int[][] pixels = get2DPixelArray(img);

        pixels = createMask2DArray(pixels, pixels);

        return pixels;
    }

    private static int[][] performBlur(BufferedImage img, Scanner scanner){
        char blur_type = getBlurType(scanner);
        int[][] pixels;

        if(blur_type == 'b'){
            pixels = boxBlur(img, getInt(scanner, "Enter a size for the box blur:"));
        } else if(blur_type == 'g'){
            pixels = gaussianBlur(img, getInt(scanner, "Enter a size for the gaussian blur: (will require larger sizes to achieve the same level of bluriness as box blur)"));
        } else{
            pixels = get2DPixelArray(img);
            System.out.println("Failed to blur image because an invalid blur type was given (somehow).");
        }

        return pixels;
    }

    private static int[][] boxBlur(BufferedImage img, int box_size){
        int[][] pixels = get2DPixelArray(img);
        int height = img.getHeight();
        int width = img.getWidth();

        for(int row = 0; row < height; row++){
            for(int col = 0; col < width; col++){
                pixels[row][col] = calculateBoxBlur(pixels, row, col, box_size);
            }
        }

        return pixels;
    }

    private static int[][] gaussianBlur(BufferedImage img, int size){
        final double[][] GAUSSIAN_DISTRIBUTION = calculateGaussianDistribution(size);
        int[][] pixels = get2DPixelArray(img);
        final int width = img.getWidth(), height = img.getHeight();
        
        for(int row = 0; row < height; row++){
            for(int col = 0; col < width; col++){
                pixels[row][col] = calculateGaussianBlur(pixels, row, col, GAUSSIAN_DISTRIBUTION);
            }
        }

        return pixels;
    }

    private static int calculateGaussianBlur(int[][] pixels, int row, int col, double[][] distribution){
        final int size = (distribution.length - 1) / 2;
        final int mid_point = size;
        int red = 0, green = 0, blue = 0;
        int pixel_colour;

        for(int x = -size; x <= size; x++){
            for(int y = -size; y <= size; y++){
                if(!(row + y < 0 || row + y >= pixels.length || col + x < 0 || col + x >= pixels[0].length)){
                    red += Math.round((pixels[row + y][col + x] & 0x000000ff) * distribution[mid_point + y][mid_point + x]);
                    green += Math.round(((pixels[row + y][col + x] & 0x0000ff00) >> 8) * distribution[mid_point + y][mid_point + x]);
                    blue += Math.round(((pixels[row + y][col + x] & 0x00ff0000) >> 16) * distribution[mid_point + y][mid_point + x]);
                }
            }
        }

        pixel_colour = (blue << 16) + (green << 8) + red;

        return pixel_colour;
    }

    private static double[][] calculateGaussianDistribution(int size){
        final double VARIANCE = 1;
        final double EULER_NUM = Math.E; 
        final double CONSTANT_PART_OF_EQUATION = (1 / (2 * Math.PI * VARIANCE * VARIANCE));
        final int mid_point = size;
        double[][] distrib = new double[size * 2 + 1][size * 2 + 1];

        for(int x = -size; x <= size; x++){
            for(int y = -size; y <= size; y++){
                distrib[mid_point + x][mid_point + y] = CONSTANT_PART_OF_EQUATION * Math.pow(EULER_NUM, - ((x * x) + (y * y)) / (2.0d * VARIANCE * VARIANCE));
            }
        }

        return distrib;
    }

    private static int calculateBoxBlur(int[][] pixels, int row, int col,int box_size){
        int red = 0, green = 0, blue = 0, average, pixel_count = 0;

        for(int y = -box_size; y <= box_size; y++){
            for(int x = -box_size; x <= box_size; x++){
                if(!(row + y < 0 || row + y >= pixels.length || col + x < 0 || col + x >= pixels[0].length)){
                    red += pixels[row + y][col + x] & 0x000000ff;
                    green += (pixels[row + y][col + x] & 0x0000ff00) >> 8;
                    blue += (pixels[row +y][col + x] & 0x00ff0000) >> 16;
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

    private static char getBlurType(Scanner scanner){
        String blur = "";
        boolean is_valid = false;

        while(!is_valid){
            blur = getString(scanner, "Choose the type of blur to perform: (b)ox blur, (g)aussian blur"); //ADD MORE BLUR OPTIONS
            
            if(blur.equals("b") || blur.equals("g")){
                is_valid = true;
            } else{
                System.out.println("Enter the character in the brackets to the left of the option.");
            }
        }

        return blur.charAt(0);
    }

    private static int[][] makeGreyscale(BufferedImage img, Scanner scanner){
        boolean use_luminance = getBoolean(scanner, "Do you want to make the image greyscale by (l)uminance or (b)rightness", new String[] {"l", "b"});
        int[][] pixels = get2DPixelArray(img);
        int grey;
        int greyscale_colour;

        for (int[] pixel_row : pixels) {
            for (int col = 0; col < pixel_row.length; col++) {
                if(use_luminance){
                    grey = calculateLuminance(pixel_row[col]);
                } else{
                    grey = calculateBrightness(pixel_row[col]);
                }

                greyscale_colour = (grey << 16) + (grey << 8) + grey;
                pixel_row[col] = greyscale_colour;
            }
        }

        return pixels;
    }

    //returns true if vertical, false if horizontal
    private static boolean getSortDirection(Scanner scanner){
        String input = "";
        boolean valid_input = false;
        
        while(!valid_input){
            input = getString(scanner, "What direction do you wish to sort pixels in, (h)orizontal or (v)ertical?");
            input = input.toLowerCase();

            if(input.equals("h") || input.equals("v")){
                valid_input = true;
            } else{
                System.out.println("You must enter either \"h\" or \"v\".");
            }
        }

        //returns false if input is h or true if not h (which means its v)
        return !input.equals("h");
    }

    private static int[] mergeSortPixels(int[] pixels, char colour_to_sort_by){
        if(pixels.length == 1){
            return pixels;
        }
        
        int mid = pixels.length / 2;
        int[] left = new int[mid];
        int[] right = new int[pixels.length - mid];

        for(int i = 0; i < mid; i++){
            left[i] = pixels[i];
        }

        for(int i = mid; i < pixels.length; i++){
            right[i - mid] = pixels[i];
        }

        left = mergeSortPixels(left, colour_to_sort_by);
        right = mergeSortPixels(right, colour_to_sort_by);

        pixels = mergePixelArrays(left, right, colour_to_sort_by);

        return pixels;
    }

    //change this so that instead of taking in a mask it takes in the colour and then write another method that does comparison so we can compare colours + brightness + luminance
    private static int[] mergePixelArrays(int[] left, int[] right, char mask_type){
        int left_pointer = 0, right_pointer = 0, result_pointer = 0;
        int[] result = new int[left.length + right.length];

        while(left_pointer < left.length && right_pointer < right.length){
            if(comparePixelProperty(left[left_pointer], right[right_pointer], mask_type)){
                result[result_pointer] = right[right_pointer];
                result_pointer++;
                right_pointer++;
            } else{
                result[result_pointer] = left[left_pointer];
                result_pointer++;
                left_pointer++;
            }
        }

        while(left_pointer < left.length){
            result[result_pointer] = left[left_pointer];
            result_pointer++;
            left_pointer++;
        }

        while(right_pointer < right.length){
            result[result_pointer] = right[right_pointer];
            result_pointer++;
            right_pointer++;
        }

        return result;
    }

    //returns true if the left pixel's property is greater than the right pixel's
    private static boolean comparePixelProperty(int left_pixel, int right_pixel, char property){
        int mask;

        mask = switch (property) {
            case 'b' -> 0x00ff0000;
            case 'g' -> 0x0000ff00;
            case 'r' -> 0x000000ff;
            default ->  0xffffffff; //this is for grey_scale and overall comparison
        };

        if(property == 'l'){
            int left_luminance, right_luminance;

            left_luminance = calculateLuminance(left_pixel);
            right_luminance = calculateLuminance(right_pixel);

            return left_luminance > right_luminance;
        } else if(property == 'h'){
            return left_pixel > right_pixel;
        } else{
            return (left_pixel & mask) > (right_pixel & mask);
        }

    }

    private static char getColourToSortBy(Scanner scanner){
        String colour = "";
        boolean valid_colour = false;

        while(!valid_colour){
            colour = getString(scanner, "What colour should the pixels be sorted by? (r)ed, (g)reen, (b)lue, (l)uminance, (h)ue");

            if(colour.equals("r") || colour.equals("g") || colour.equals("b") || colour.equals("l") || colour.equals("h")){
                valid_colour = true;
            } else{
                System.out.println("You must enter either \"r\", \"g\", \"b\", \"l\", or \"h\".");
            }
        }

        return colour.charAt(0);
    }

    private static boolean useMask(Scanner scanner){
        String response = "";
        boolean valid = false;

        while(!valid){
            response = getString(scanner, "Should a mask be used? (y/n)");
            response = response.toLowerCase();

            if(response.equals("y") || response.equals("n")){
                valid = true;
            } else{
                System.out.println("You must enter either \"y\" or \"n\".");
            }
        }

        return response.equals("y");
    }

    // GENERIC METHODS
    // get2DPixelArray, getString, getInt, createImage, getImageFile, saveImageFile, checkIfInt, calculateLuminance, arrayContainsString
    //#region

    private static int[][] get2DPixelArray(BufferedImage img){
        Raster image_raster = img.getData();
        int width = image_raster.getWidth(), height = image_raster.getHeight();

        int[][] pixels = new int[height][width];
        int values_per_pixel = (img.getAlphaRaster() != null) ? 4 /* rgba */: 3 /* rgb */;
        int[] temp = new int[values_per_pixel];

        for(int row = 0; row < height; row++){
            for(int col = 0; col < width; col++){
                temp = image_raster.getPixel(col, row, temp);
                // returns rgba

                //abgr
                if(values_per_pixel == 4){
                    pixels[row][col] = ((temp[3] & 0xff) << 24) + ((temp[2] & 0xff) << 16) + ((temp[1] & 0xff) << 8) + (temp[0] & 0xff);
                } else{ //bgr
                    pixels[row][col] = (temp[2] << 16) + (temp[1] << 8) + temp[0];
                }
            }
        }

        return pixels;
    }

    private static WritableRaster createImage(int[][] sorted_pixels, BufferedImage img){
        WritableRaster raster = img.getRaster();
        int width = raster.getWidth(), height = raster.getHeight();

        int values_per_pixel = (img.getAlphaRaster() != null) ? 4 : 3;
        int[] temp = new int[values_per_pixel];

        for(int row = 0; row < height; row++){
            for(int col = 0; col < width; col++){
                temp[0] = (sorted_pixels[row][col] & 0x000000ff);
                temp[1] = (sorted_pixels[row][col] & 0x0000ff00) >> 8;
                temp[2] = (sorted_pixels[row][col] & 0x00ff0000) >> 16;

                if(values_per_pixel == 4){
                    temp[3] = (sorted_pixels[row][col] & 0xff000000) >> 24;
                }

                raster.setPixel(col, row, temp);
            }
        }

        return raster;
    }

    private static int calculateBrightness(int abgr){
        int red, green, blue;
        red = abgr & 0x000000FF;
        green = (abgr & 0x0000FF00) >> 8;
        blue = (abgr & 0x00FF0000) >> 16;
        
        return (red + green + blue) / 3;
    }

    private static int calculateLuminance(int abgr){
        int red, green, blue;
        int red_coefficient = (int) Math.round(0.2126 * 255);
        int green_coefficient = (int) Math.round(0.7152 * 255);
        int blue_coefficient = (int) Math.round(0.0722 * 255);
        
        red = abgr & 0x000000FF;
        green = (abgr & 0x0000FF00) >> 8;
        blue = (abgr & 0x00FF0000) >> 16;

        return (red * red_coefficient + green * green_coefficient + blue * blue_coefficient) / 255;
    }

    private static void saveImage(String file_name, WritableRaster img) throws IOException{
        File img_file = new File(file_name + ".png");
        int type = img.getNumBands() == 4 ? BufferedImage.TYPE_4BYTE_ABGR : BufferedImage.TYPE_3BYTE_BGR;
        BufferedImage buffer = new BufferedImage(img.getWidth(), img.getHeight(), type);
        buffer.setData(img);
        ImageIO.write(buffer, "png", img_file);
    }

    private static boolean arrayContainsString(String[] array, String target){
        for (String string_in_array : array) {
            if (string_in_array.equals(target)) {
                return true;
            }
        }

        return false;
    }

    private static String getString(Scanner scanner, String message){
        System.out.println(message);
        return scanner.nextLine();
    }

    private static File getImageFile(Scanner scanner){
        String image_path = getString(scanner, "Enter the full path of the image you want to pixel sort.");
        File image = new File(image_path);
        
        while(!isFileValid(image)){
            System.out.println("File path is invalid, please try again.");
            image_path = getString(scanner, "Enter the full path of the image you want to pixel sort.");
            image = new File(image_path);
        }

        return new File(image_path);
    }

    private static int getInt(Scanner scanner, String message){
        String input;
        int input_as_int = 0;
        boolean is_valid = false;

        while(!is_valid){
            System.out.println(message);
            input = scanner.nextLine();

            if(checkIfInt(input)){
                input_as_int = Integer.parseInt(input);
                is_valid = true;
            } else{
                System.out.println("You must enter an integer.");
            }
        }

        return input_as_int;
    }

    /**
     * 
     * @param scanner
     * @param message
     * @param valid_responses // the first element must be the true one and there must be at least 2 elements
     * @return boolean
     */
    private static boolean getBoolean(Scanner scanner, String message, String[] valid_responses){
        String input;
        boolean return_value = false;
        boolean is_valid = false;

        while(!is_valid){
            input = getString(scanner, message);

            if(input.equals(valid_responses[0])){
                return_value = true;
                is_valid = true;
            } else{
                for(int i = 1; i < valid_responses.length;i++){
                    if(input.equals(valid_responses[i])){
                        return_value = false;
                        is_valid = true;
                    }
                }
            }

            if(!is_valid){
                System.out.println("That is not a valid response. You must type one of the following:");
                for(String response : valid_responses){
                    System.out.println("\"" + response +"\"");
                }
            }
        }

        return return_value;
    }

    private static boolean checkIfInt(String s){
        boolean valid_char;

        for(int i = 0; i < s.length(); i++){
            valid_char = false;
            if('0' <= s.charAt(i) && s.charAt(i) <= '9'){
                valid_char = true;
            } else if(i == 0 && s.charAt(i) == '-'){
                valid_char = true;
            }

            if(!valid_char){
                return false;
            }
        }

        return true;
    }

    private static boolean isFileValid(File file){
        return file.exists();
    }

    //#endregion
}
