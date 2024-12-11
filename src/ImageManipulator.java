import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.IOException;
import java.util.Scanner;
import javax.imageio.ImageIO;

public class ImageManipulator {
    public static void main(String[] args) throws IOException {
        Scanner scanner = new Scanner(System.in);
        File image_file = getImageFile(scanner);
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

        img = createImage(pixel_array, img);

        file_name = getString(scanner, "What do you wish to save your file as? (do not include filetype)");
        saveImage(file_name, img);
    }
    
    private static int[][] sortPixels(BufferedImage img, Scanner scanner){
        int[][] pixels = get2DPixelArray(img);
        int[][] mask_of_pixels = new int[pixels.length][pixels[0].length];
        int[] temp_array;
        int height = img.getHeight(), width = img.getWidth(); 
        final int lower_threshold = 0x33, upper_threshold = 0xcc;
        char sorting_property = getColourToSortBy(scanner);
        boolean sort_vertical;
        int starting_point, length;

        mask_of_pixels = createMask2DArray(pixels, mask_of_pixels, lower_threshold, upper_threshold);

        sort_vertical = getSortDirection(scanner);  

        if(sort_vertical){

        } else{
            for(int row = 0; row < height; row ++){
                starting_point = -1;
                length = 0;

                for(int col = 0; col < width; col++){
                    if(mask_of_pixels[row][col] != 0 && starting_point == -1){
                        starting_point = col;
                        length++;
                    } else if(mask_of_pixels[row][col] != 0){
                        length++;
                    } else if(mask_of_pixels[row][col] == 0 && starting_point != -1){

                        temp_array = new int[length];

                        for(int i = 0; i < length; i++){
                            temp_array[i] = pixels[row][starting_point + i]; 
                        }

                        temp_array = mergeSortPixels(temp_array, sorting_property);

                        for(int i = 0; i < length; i++){
                            pixels[row][starting_point + i] = temp_array[i];
                        }

                        starting_point = -1;
                        length = 0;
                    }
                }
            }
        }

        return pixels;
    }

    private static int[][] createMask2DArray(int[][] pixels, int[][] array_to_write_to,int lower_threshold, int upper_threshold){
        for(int row = 0; row < pixels.length; row++){
            for(int col = 0; col < pixels[0].length; col++){
                int luminance = calculateLuminance(pixels[row][col]);

                if(lower_threshold < luminance && luminance < upper_threshold){
                    array_to_write_to[row][col] = 0x00ffffff;
                } else{
                    array_to_write_to[row][col] = 0x00000000;
                }
            }
        }

        return array_to_write_to;
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

    private static int[][] createMask(BufferedImage img) throws IOException{
        int[][] pixels = get2DPixelArray(img);
        //these are arbitrary values
        final int lower_threshold = 0x33;
        final int upper_threshold = 0xaa;

        pixels = createMask2DArray(pixels, pixels, lower_threshold, upper_threshold);

        return pixels;
    }

    private static int[][] performBlur(BufferedImage img, Scanner scanner) throws IOException{
        char blur_type = getBlurType(scanner);
        int[][] pixels;

        if(blur_type == 'b'){
            pixels = boxBlur(img, getInt(scanner, "Enter a size for the box blur:"));
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
            blur = getString(scanner, "Choose the type of blur to perform: (b)ox blur"); //ADD MORE BLUR OPTIONS
            
            if(blur.equals("b")){
                is_valid = true;
            } else{
                System.out.println("Enter the character in the brackets to the left of the option.");
            }
        }

        return blur.charAt(0);
    }

    private static int[][] makeGreyscale(BufferedImage img, Scanner scanner) throws IOException{
        int[][] pixels = get2DPixelArray(img);
        int luminance;
        int greyscale_colour;

        for (int[] pixel_row : pixels) {
            for (int col = 0; col < pixel_row.length; col++) {
                luminance = calculateLuminance(pixel_row[col]);
                greyscale_colour = (luminance << 16) + (luminance << 8) + luminance;
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

    private static void pixelSort(Scanner scanner) throws IOException{
        File img_file = getImageFile(scanner);
        boolean sort_vertical = getSortDirection(scanner);
        int[][] pixel_array;
        char colour_sort;
        String file_name;

        BufferedImage img = ImageIO.read(img_file);
        pixel_array = get2DPixelArray(img);

        colour_sort = getColourToSortBy(scanner);
        pixel_array = sortPixels(pixel_array, sort_vertical, colour_sort);

        img = createImage(pixel_array, img);
        file_name = getString(scanner, "What do you wish to save your file as? (do not include file type)");

        saveImage(file_name, img);
    }

    private static int[][] sortPixels(int[][] pixels, boolean sort_vertical, char colour_to_sort_by) throws IOException{

        int width = pixels[0].length, height = pixels.length;

        if(sort_vertical){
            //eventually do this

        } else{
            for(int i = 0; i < height; i++){
                pixels[i] = mergeSortPixels(pixels[i], colour_to_sort_by);
            }
        }

        return pixels;
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
            right[i] = pixels[mid + i];
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
            case 'b' -> 0x00FF0000;
            case 'g' -> 0x0000FF00;
            case 'r' -> 0x000000FF;
            default -> 0xFFFFFFFF; //this is for grey_scale and overall comparison
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

    // GENERIC METHODS
    // get2DPixelArray, getString, getInt, createImage, getImageFile, saveImageFile, checkIfInt, calculateLuminance, arrayContainsString
    //#region

    private static int[][] get2DPixelArray(BufferedImage img){
        byte[] pixel_data = ((DataBufferByte) img.getRaster().getDataBuffer()).getData();
        int width = img.getWidth(), height = img.getHeight();
        
        //checks if the image has an alpha channel by checking if you can write to it.
        boolean has_alpha = img.getAlphaRaster() != null;
        int[][] result = new int[height][width];
        
        //the number of values stored by each pixel
        int number_of_values; 

        if(has_alpha){
            number_of_values = 4;

            for(int value_pointer = 0, row = 0, col = 0; value_pointer + number_of_values - 1 < pixel_data.length; value_pointer += number_of_values){
                //pixel data is stored as abgr
                int abgr = 0; //value stored for pixel (8 bits for each alpha, blue, green, red)
                abgr += (((int) pixel_data[value_pointer]) & 0xff) << 24;
                abgr += (((int) pixel_data[value_pointer + 1]) & 0xff) << 16;
                abgr += (((int) pixel_data[value_pointer + 2]) & 0xff) << 8;
                abgr += (((int) pixel_data[value_pointer + 3]) & 0xff);

                result[row][col] = abgr;

                col++;

                if(col == width){
                    col = 0;
                    row++;
                }
            }
        } else{
            number_of_values = 3;

            for(int value_pointer = 0, row = 0, col = 0; value_pointer + number_of_values - 1 < pixel_data.length; value_pointer += number_of_values){
                //pixel data is stored as bgr
                int bgr = 0;

                bgr += (((int) pixel_data[value_pointer ]) & 0xff) << 16;
                bgr += (((int) pixel_data[value_pointer + 1]) & 0xff) << 8;
                bgr += (((int) pixel_data[value_pointer + 2]) & 0xff);

                result[row][col] = bgr;

                col++;

                if(col == width){
                    col = 0;
                    row++;
                }
            }
        }

        return result;
    }

    private static BufferedImage createImage(int[][] sorted_pixels, BufferedImage img){
        byte[] image_buffer = ((DataBufferByte) img.getRaster().getDataBuffer()).getData();

        int width = img.getWidth();
        int height = img.getHeight();

        byte pixel_element;
        boolean has_alpha = img.getAlphaRaster() != null;
        int pointer = 0;
        int multiplier = has_alpha ? 4 : 3;
        int row = 0, col = 0;

        do{

            if(has_alpha){
                pixel_element = (byte)((sorted_pixels[row][col] & 0xFF000000) >> 24);
                image_buffer[pointer] = pixel_element;
            
                pixel_element = (byte)((sorted_pixels[row][col] & 0x00FF0000) >> 16);
                image_buffer[pointer + 1] = pixel_element;
            
                pixel_element = (byte)((sorted_pixels[row][col] & 0x0000FF00) >> 8);
                image_buffer[pointer + 2] = pixel_element;

                pixel_element = (byte)(sorted_pixels[row][col] & 0x000000FF);
                image_buffer[pointer + 3] = pixel_element;
            } else{

                pixel_element = (byte)((sorted_pixels[row][col] & 0x00FF0000) >> 16);
                image_buffer[pointer] = pixel_element;
            
                pixel_element = (byte)((sorted_pixels[row][col] & 0x0000FF00) >> 8);
                image_buffer[pointer + 1] = pixel_element;
            
                pixel_element = (byte)(sorted_pixels[row][col] & 0x000000FF);
                image_buffer[pointer + 2] = pixel_element;    
            }

            col++;

            if(col == width){
                col = 0;
                row++;
            }

            pointer = (row * width + col) * multiplier;
        } while(row < height);

        return img;
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

    private static void saveImage(String file_name, BufferedImage img) throws IOException{
        File img_file = new File(file_name + ".png");
        ImageIO.write(img, "png", img_file);
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
        String image_path = getString(scanner, "Enter the full path of the file you want to pixel sort.");
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

    //#endregion
}
