# Basic Image Manipulation

## Plans
- Add Gaussian blur
- Add option to choose to sort by ascending or descending
- Add edge-detection option
- Add resizing option (up and down)
    - Nearest-neigbour
    - Box sampling
    - Biliniear
    - Bicubic
- Add image cropping option
- Add more masking options
    - Custom thresholds
    - Mask based off rgb values as well as luminance

## Known Bugs / Issues
- Box blur is VERY slow for larger box sizes (not a priority to fix)

## Features:
- Horizontal and vertical pixel sorting (with mask and no mask)
- Box blur of variable sized boxes
- Make an image greyscale based off luminance and brightness
- Make a mask of an image with automatically calculated thresholds (Otsu's method) 