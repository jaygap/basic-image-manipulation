# Basic Image Manipulation

## Plans
- Add option to choose to sort by ascending or descending
- Add edge-detection option
    - Canny
    - Kovalevsky
    - Sobel operator
- Add resizing option (up and down)
    - Nearest-neigbour
    - Box sampling
    - Biliniear
    - Bicubic
- Add image cropping option
- Add more masking options [being worked on]
    - Custom thresholds

## Known Bugs / Issues
- Box blur is VERY slow for larger box sizes (not a priority to fix)
- Gaussian blur also slow
- Hue masking results in very blocky textures for some reason

## Features:
- Horizontal and vertical pixel sorting (with mask and no mask)
- Box blur of variable sized boxes
- Make an image greyscale based off luminance and brightness
- Make a mask of an image with automatically calculated thresholds (Otsu's method) 
    - Supports luminance and rgb (each component separately)
- Gaussian blur