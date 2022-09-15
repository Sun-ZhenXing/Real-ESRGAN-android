#ifndef MAIN_H
#define MAIN_H

#define DLL_IMPORT extern "C"

DLL_IMPORT int esrgan(int argc, char** argv);
DLL_IMPORT int waifu2x(int argc, char** argv);

#endif
