(*^
::[	Information =

	"This is a Mathematica Notebook file.  It contains ASCII text, and can be
	transferred by email, ftp, or other text-file transfer utility.  It should
	be read or edited using a copy of Mathematica or MathReader.  If you 
	received this as email, use your mail application or copy/paste to save 
	everything from the line containing (*^ down to the line containing ^*)
	into a plain text file.  On some systems you may have to give the file a 
	name ending with ".ma" to allow Mathematica to recognize it as a Notebook.
	The line below identifies what version of Mathematica created this file,
	but it can be opened using any other version as well.";

	FrontEndVersion = "Macintosh Mathematica Notebook Front End Version 2.2";

	MacintoshStandardFontEncoding; 
	
	fontset = title, inactive, noPageBreakBelow, nohscroll, preserveAspect, groupLikeTitle, center, M7, bold, e8,  24, "Times"; 
	fontset = subtitle, inactive, noPageBreakBelow, nohscroll, preserveAspect, groupLikeTitle, center, M7, bold, e6,  18, "Times"; 
	fontset = subsubtitle, inactive, noPageBreakBelow, nohscroll, preserveAspect, groupLikeTitle, center, M7, italic, e6,  14, "Times"; 
	fontset = section, inactive, noPageBreakBelow, nohscroll, preserveAspect, groupLikeSection, grayBox, M22, bold, a20,  18, "Times"; 
	fontset = subsection, inactive, noPageBreakBelow, nohscroll, preserveAspect, groupLikeSection, blackBox, M19, bold, a15,  14, "Times"; 
	fontset = subsubsection, inactive, noPageBreakBelow, nohscroll, preserveAspect, groupLikeSection, whiteBox, M18, bold, a12,  12, "Times"; 
	fontset = text, inactive, nohscroll, noKeepOnOnePage, preserveAspect, M7,  12, ""; 
	fontset = smalltext, inactive, nohscroll, noKeepOnOnePage, preserveAspect, M7,  10, "Times"; 
	fontset = input, noPageBreakBelow, nowordwrap, preserveAspect, groupLikeInput, M42, N23, bold,  12, "Courier"; 
	fontset = output, output, inactive, noPageBreakBelow, nowordwrap, preserveAspect, groupLikeOutput, M42, N23, L-4,  12, "Courier"; 
	fontset = message, inactive, noPageBreakBelow, nowordwrap, preserveAspect, groupLikeOutput, M42, N23,  12, "Courier"; 
	fontset = print, inactive, noPageBreakBelow, nowordwrap, preserveAspect, groupLikeOutput, M42, N23,  12, "Courier"; 
	fontset = info, inactive, noPageBreakBelow, nowordwrap, preserveAspect, groupLikeOutput, M42, N23,  12, "Courier"; 
	fontset = postscript, PostScript, formatAsPostScript, output, inactive, noPageBreakBelow, nowordwrap, preserveAspect, groupLikeGraphics, M7, l34, w282, h287,  12, "Courier"; 
	fontset = name, inactive, nohscroll, noKeepOnOnePage, preserveAspect, M7, italic,  10, "Times"; 
	fontset = header, inactive, nohscroll, noKeepOnOnePage, preserveAspect, M7,  12, ""; 
	fontset = leftheader, inactive, L2,  12, "Times"; 
	fontset = footer, inactive, nohscroll, noKeepOnOnePage, preserveAspect, center, M7,  12, ""; 
	fontset = leftfooter, inactive, L2,  12, "Times"; 
	fontset = help, inactive, nohscroll, noKeepOnOnePage, preserveAspect, M7,  10, "Times"; 
	fontset = clipboard, inactive, nohscroll, noKeepOnOnePage, preserveAspect, M7,  12, ""; 
	fontset = completions, inactive, nohscroll, noKeepOnOnePage, preserveAspect, M7,  12, "Courier"; 
	fontset = special1, inactive, nohscroll, noKeepOnOnePage, preserveAspect, M7,  14, "Times"; 
	fontset = special2, inactive, nohscroll, noKeepOnOnePage, preserveAspect, M7,  12, ""; 
	fontset = special3, inactive, nohscroll, noKeepOnOnePage, preserveAspect, M7,  12, ""; 
	fontset = special4, inactive, nohscroll, noKeepOnOnePage, preserveAspect, M7,  12, ""; 
	fontset = special5, inactive, nohscroll, noKeepOnOnePage, preserveAspect, M7,  12, ""; 
	paletteColors = 128; automaticGrouping; currentKernel; 
]
:[font = title; inactive; preserveAspect; startGroup]
Example 16
Surface, Contour, and Density Plots
:[font = subsubtitle; inactive; preserveAspect]
Copyright ã 1993 by Bill Titus, Carleton College, 
Department of Physics and Astronomy, Northfield, MN  55057-4025
September 6, 1993
;[s]
3:0,0;10,1;11,0;133,-1;
2:2,16,12,Times,2,14,0,0,0;1,16,12,Symbol,0,14,0,0,0;
:[font = section; inactive; preserveAspect; startGroup]
Topics and Skills
:[font = special1; inactive; preserveAspect; endGroup]
1.  Plot3D[] with options:  LightSource,  AxesLabel,  Boxed,  Axes,  PlotPoints,  and  ViewPoint.
2.  ContourPlot[]  with various options.
3.  DensityPlot[]  with various options.
;[s]
19:0,0;4,1;12,0;28,1;39,0;42,1;51,0;54,1;59,0;62,1;66,0;69,1;79,0;87,1;96,0;102,1;115,0;143,1;156,0;180,-1;
2:10,16,12,Times,0,14,0,0,0;9,15,11,Courier,0,14,0,0,0;
:[font = section; inactive; preserveAspect; startGroup]
Problem
:[font = special1; inactive; preserveAspect]
A rubber sheet is stretched tight over the square boundary   0 < x < a   and      
0 < y < a.  The normal modes of oscillation for such a sheet are of the form
:[font = special1; inactive; preserveAspect]
         z[x, y] = cos(wt + a) sin(k1x) sin(k2y)
;[s]
10:0,0;8,1;23,2;24,1;28,2;29,1;36,3;37,1;45,3;46,1;49,-1;
4:1,16,12,Times,0,14,0,0,0;5,15,11,Courier,0,14,0,0,0;2,16,12,Symbol,0,14,0,0,0;2,23,15,Courier,64,14,0,0,0;
:[font = special1; inactive; preserveAspect]
Here  a  is a phase constant and
;[s]
3:0,0;6,1;7,0;33,-1;
2:2,16,12,Times,0,14,0,0,0;1,16,12,Symbol,0,14,0,0,0;
:[font = special1; inactive; preserveAspect]
          k1 = 2pn/a,  k2 = 2pm/a,  w2 = c2(k12 + k22),                      
;[s]
22:0,0;10,1;11,2;12,1;16,3;17,1;24,2;25,1;29,3;30,1;36,3;37,4;38,1;42,4;43,1;45,2;46,4;47,1;51,2;52,4;53,1;56,0;78,-1;
5:2,16,12,Times,0,14,0,0,0;9,15,11,Courier,0,14,0,0,0;4,23,15,Courier,64,14,0,0,0;3,16,12,Symbol,0,14,0,0,0;4,23,15,Courier,32,14,0,0,0;
:[font = special1; inactive; preserveAspect; endGroup]
where  n  and  m  are positive integers, and  c  is the wave speed.  Examine graphically some of the normal modes of oscillation for this system.
;[s]
7:0,0;7,1;8,0;15,1;16,0;46,1;47,0;146,-1;
2:4,16,12,Times,0,14,0,0,0;3,15,11,Courier,0,14,0,0,0;
:[font = section; inactive; preserveAspect; startGroup]
Solution
:[font = subsection; inactive; preserveAspect]
Step 1 - For simplicity, assume that  a = 1,   c = 1,  and  a = 0.  Use the MMA command  Plot3D[]  to plot the surface of the sheet at time  t = 0  for the normal mode corresponding to  n = 1  and  m = 2.  Don't include any options in the plot command. 
;[s]
5:0,0;60,1;61,0;89,2;97,0;254,-1;
3:3,16,12,Times,1,14,0,0,0;1,16,12,Symbol,0,14,0,0,0;1,15,11,Courier,1,14,0,0,0;
:[font = subsection; inactive; preserveAspect; startGroup]
Comment 1
:[font = special1; inactive; preserveAspect; endGroup]
1.  Why might you want to evaluate  w  numerically before calling  Plot3D[]?
;[s]
3:0,0;67,1;75,0;77,-1;
2:2,16,12,Times,0,14,0,0,0;1,15,11,Courier,0,14,0,0,0;
:[font = subsection; inactive; preserveAspect]
Step 2 - Examine the options for  Plot3D[]  and use some of them to label your axes and change to surface color to red. 
;[s]
3:0,0;34,1;42,0;121,-1;
2:2,16,12,Times,1,14,0,0,0;1,15,11,Courier,1,14,0,0,0;
:[font = subsection; inactive; preserveAspect]
Step 3 - Now choose options to remove the axes and the bounding box so that the sheet just sits there in free space.  Keep the surface color red, but increase the number of  PlotPoints  to  30  so that the surface is smoother.
;[s]
5:0,0;174,1;184,0;190,1;192,0;227,-1;
2:3,16,12,Times,1,14,0,0,0;2,15,11,Courier,1,14,0,0,0;
:[font = subsection; inactive; preserveAspect; startGroup]
Comment 3
:[font = special1; inactive; preserveAspect; endGroup]
1.  Note that if  Axes  is set to  False, then  AxesLabel  is simply ignored.
2.  The points are sampled uniformly.  The default number of points sampled is  15 x 15  so that  30 x 30  is four times as many sample points.
;[s]
11:0,0;18,1;22,0;35,1;40,0;48,1;57,0;161,2;162,0;179,2;180,0;222,-1;
3:6,16,12,Times,0,14,0,0,0;3,15,11,Courier,0,14,0,0,0;2,15,11,Helvetica,0,14,0,0,0;
:[font = subsection; inactive; preserveAspect]
Step 4 -  Practice changing the viewpoint at which you observe the free surface.  You might want to use the 3D ViewPoint Selector to help you out.
:[font = subsection; inactive; preserveAspect]
Step 5 - Create a table of ten plots for  t   going from  0  to  2Pi/w.  Then animate your surface's motion as a function of time.  To speed up the drawing, set  PlotPoints  to  15.
;[s]
11:0,0;42,1;43,0;58,1;59,0;65,1;70,0;162,1;172,0;178,1;180,0;182,-1;
2:6,16,12,Times,1,14,0,0,0;5,15,11,Courier,1,14,0,0,0;
:[font = subsection; inactive; preserveAspect]
Step 6 - Now make a contour plot of the surface at time  t = 0  for the normal mode corresponding to  n = 1  and  m = 1.  As before, assume that  a = 1,  c = 1,  and  a = 0.  Locate the appropriate MMA command to accomplish this task, and apply it without any of its options.
;[s]
3:0,0;167,1;168,0;276,-1;
2:2,16,12,Times,1,14,0,0,0;1,16,12,Symbol,0,14,0,0,0;
:[font = subsection; inactive; preserveAspect]
Step 7 -  Play with some of the options to  ContourPlot[]  to enchance the appearance of the normal mode plot.
;[s]
3:0,0;44,1;57,0;111,-1;
2:2,16,12,Times,1,14,0,0,0;1,15,11,Courier,1,14,0,0,0;
:[font = subsection; inactive; preserveAspect; startGroup]
Comment 7
:[font = special1; inactive; preserveAspect; endGroup]
1.  The options  ContourSmoothing  may significantly increases the time it takes to produce the contour plot.
;[s]
3:0,0;17,1;33,0;110,-1;
2:2,16,12,Times,0,14,0,0,0;1,15,11,Courier,0,14,0,0,0;
:[font = subsection; inactive; preserveAspect]
Step 8 - Repeat Step 7, but now use the MMA command  DensityPlot[]  and some of its options.   
;[s]
3:0,0;53,1;66,0;96,-1;
2:2,16,12,Times,1,14,0,0,0;1,15,11,Courier,1,14,0,0,0;
:[font = subsection; inactive; preserveAspect; startGroup]
Comment 8 
:[font = special1; inactive; preserveAspect; endGroup; endGroup]
1.  Be sure to try the option  Mesh -> False  and see what happens when you increase the number of  PlotPoints  to  100.
2.  Note that for printing to the laser printer that doesn't print in color, it's best to use shades of grays and not colors for your plots.
;[s]
7:0,0;31,1;44,0;100,1;110,0;116,1;119,0;262,-1;
2:4,16,12,Times,0,14,0,0,0;3,15,11,Courier,0,14,0,0,0;
:[font = section; inactive; preserveAspect; startGroup]
Save Your Notebook
:[font = special1; inactive; preserveAspect; endGroup; endGroup]
Remove any output cells from this notebook and then use  Save As  to store your notebook under the name   myEx16.
;[s]
5:0,0;57,1;64,0;106,1;112,0;114,-1;
2:3,16,12,Times,0,14,0,0,0;2,15,11,Helvetica,0,14,0,0,0;
^*)
