# Basic Image Manipulation

## Plans
- Add edge-detection option
    - Canny
    - Kovalevsky
- Add resizing option (up and down)
    - Nearest-neigbour
    - Box sampling
    - Biliniear
    - Bicubic
- Add image cropping option

## Known Bugs / Issues
- Box blur is VERY slow for larger box sizes (not a priority to fix)
- Gaussian blur also slow
- Hue masking results in very blocky textures for some reason

## Features:
- Pixel sorting options:
    - Horizontal and vertical
    - Ascending and descending
    - Sort by red, green, blue, luminance and hue
    - Sort according to a mask
        - Based off red, green, blue, luminance or hue
- Blur types:
    - Box
    - Gaussian
- Make an image greyscale based off luminance and brightness
- Make a mask of an image with automatically calculated thresholds (Otsu's method) 
    - Supports luminance and rgb (each component separately)
- Make a mask of an image with a designated threshold
- Edge detection