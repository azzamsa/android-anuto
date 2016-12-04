package ch.logixisland.anuto.game.render;

import android.graphics.Matrix;

import ch.logixisland.anuto.util.math.vector.Vector2;

public class Viewport {

    private final Vector2 mGameSize = new Vector2(10, 10);
    private final Vector2 mScreenSize = new Vector2(100, 100);
    private final Matrix mScreenMatrix = new Matrix();
    private final Matrix mScreenMatrixInverse = new Matrix();

    public Vector2 getGameSize() {
        return new Vector2(mGameSize);
    }

    public void setGameSize(int width, int height) {
        mGameSize.set(width, height);
        calcScreenMatrix();
    }

    public void setScreenSize(int width, int height) {
        mScreenSize.set(width, height);
        calcScreenMatrix();
    }

    public Matrix getScreenMatrix() {
        return mScreenMatrix;
    }

    public Vector2 screenToGame(Vector2 pos) {
        float[] pts = {pos.x, pos.y};
        mScreenMatrixInverse.mapPoints(pts);
        return new Vector2(pts[0], pts[1]);
    }

    public boolean inGame(Vector2 pos) {
        return pos.x >= -0.5f && pos.y >= -0.5f &&
                pos.x < mGameSize.x + 0.5f && pos.y < mGameSize.y + 0.5f;
    }


    private void calcScreenMatrix() {
        mScreenMatrix.reset();

        float tileSize = Math.min(mScreenSize.x / mGameSize.x, mScreenSize.y / mGameSize.y);
        mScreenMatrix.postTranslate(0.5f, 0.5f);
        mScreenMatrix.postScale(tileSize, tileSize);

        float paddingLeft = (mScreenSize.x - (tileSize * mGameSize.x)) / 2f;
        float paddingTop = (mScreenSize.y - (tileSize * mGameSize.y)) / 2f;
        mScreenMatrix.postTranslate(paddingLeft, paddingTop);

        mScreenMatrix.postScale(1f, -1f);
        mScreenMatrix.postTranslate(0, mScreenSize.y);

        mScreenMatrix.invert(mScreenMatrixInverse);
    }

}
