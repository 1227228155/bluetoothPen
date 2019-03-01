package com.sonix.oidbluetooth;

public class Dots {
    Dots(int BookID, int PageID, float x, float y, int f, int t, int width, int color, int counter, int angle) {
        bookID = BookID;
        pageID = PageID;
        pointX = x;
        pointY = y;
        force = f;
        ntype = t;
        penWidth = width;
        ncolor = color;
        ncounter = counter;
        nangle = angle;
    }

    int bookID;
    int pageID;
    int ncounter;
    float pointX;
    float pointY;
    int force;
    int ntype;  //0-down;1-move;2-up;
    int penWidth;
    int ncolor;
    int nangle;
}
