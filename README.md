# Basic Image Manipulation

## Plans
- Fix pixel sorting bug
- Add Gaussian blur
- Add vertical pixel sorting
- Add option to choose to sort by ascending or descending
- Add edge-detection option
- Add resizing option (up and down)
    - Nearest-neigbour
    - Box sampling
    - Biliniear
    - Bicubic
- Add image cropping option
- Add more masking options
    - Variable thresholds
    - Mask based off rgb values as well as luminance

## Known Bugs / Issues
- Pixel sorting adds black pixels on the left for some reason
- Box blur is VERY slow for larger box sizes (not a priority to fix)

## Features:
- Horizontal Pixel Sorting based on a mask created from the image (currently bugged)
- Box blur of variable sized boxes
- Make an image greyscale based off luminance and brightness
- Make a mask of an image with fixed thresholds based on luminance 