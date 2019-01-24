# javacv-javacpp-ffmpeg
Record and Stream with javacv-javacpp-ffmpeg

> mvn compile exec:java

build jar for alpine

> url https://github.com/bytedeco/javacpp-presets/wiki/Building-FFMPEG-JavaCPP-preset-on-Ubuntu-16.04
> gcc g++ git bash curl tar make patch perl 注掉openssl cmake diffutils libxcb libxcb-dev 
> 修改javacpp generater.java中的malloc_trim
> pulseaudio-dev --enable-libpulse
