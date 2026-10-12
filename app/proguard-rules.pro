# Application-specific R8 rules.
-keep class it.elledue.control.data.**Entity { *; }
-keep class it.elledue.control.reports.ReportSnapshot { *; }
-keepattributes Signature,*Annotation*
# PDFBox's optional JPEG-2000 codec is not bundled: reports use bundled PNG logos
# and embedded fonts; the app neither imports JP2 images nor opens arbitrary PDFs.
-dontwarn com.gemalto.jp2.JP2Decoder
-dontwarn com.gemalto.jp2.JP2Encoder
