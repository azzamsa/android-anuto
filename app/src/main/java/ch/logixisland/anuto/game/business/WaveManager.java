package ch.logixisland.anuto.game.business;

import android.os.Handler;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import ch.logixisland.anuto.game.GameEngine;
import ch.logixisland.anuto.game.TickTimer;
import ch.logixisland.anuto.game.data.EnemyDescriptor;
import ch.logixisland.anuto.game.data.Wave;
import ch.logixisland.anuto.game.entity.Entity;
import ch.logixisland.anuto.game.entity.enemy.Enemy;
import ch.logixisland.anuto.util.math.MathUtils;

public class WaveManager {

    /*
    ------ Listener Interface ------
     */

    public interface Listener {
        void onStarted(WaveManager m);
        void onAborted(WaveManager m);
        void onFinished(WaveManager m);
        void onEnemyAdded(WaveManager m, Enemy e);
        void onEnemyRemoved(WaveManager m, Enemy e);
    }

    /*
    ------ Members ------
     */

    private GameEngine mGame = GameEngine.getInstance();
    private GameManager mManager = GameManager.getInstance();

    private Wave mWave;
    private int mExtend;
    private boolean mAborted;
    private int mEnemiesRemaining;
    private int mEarlyBonus;

    private float mHealthModifier;
    private float mRewardModifier;

    private volatile int mWaveReward;

    private List<Listener> mListeners = new CopyOnWriteArrayList<>();

    /*
    ------ Entity.Listener Implementation ------
     */

    private Entity.Listener mObjectListener = new Entity.Listener() {
        @Override
        public void onObjectAdded(Entity obj) {
        }

        @Override
        public void onObjectRemoved(Entity obj) {
            mEnemiesRemaining--;
            mEarlyBonus -= ((Enemy)obj).getReward();

            if (mEnemiesRemaining == 0 && !mAborted) {
                onFinished();
            }

            onEnemyRemoved((Enemy)obj);
        }
    };

    /*
    ------ Constructors ------
     */

    public WaveManager(Wave wave, int extend) {
        mWave = wave;
        mExtend = extend;

        mWaveReward = mWave.getWaveReward();
        mHealthModifier = mWave.getHealthModifier();
        mRewardModifier = mWave.getRewardModifier();
    }

    /*
    ------ Methods ------
     */

    public Wave getWave() {
        return mWave;
    }

    public int getEarlyBonus() {
        return mEarlyBonus;
    }

    public int getExtend() {
        return mExtend;
    }

    public float getHealthModifier() {
        return mHealthModifier;
    }

    public void modifyHealth(float modifier) {
        mHealthModifier *= modifier;
    }

    public float getRewardModifier() {
        return mRewardModifier;
    }

    public void modifyReward(float modifier) {
        mRewardModifier *= modifier;
    }

    public void modifyWaveReward(int modifier) {
        mWaveReward *= modifier;
    }

    public void start() {
        mGame.add(new Runnable() {
            @Override
            public void run() {
                int delay = 0;
                float offsetX = 0f;
                float offsetY = 0f;

                mAborted = false;
                mEnemiesRemaining = mWave.getEnemies().size() * (mExtend + 1);

                for (int i = 0; i < mExtend + 1; i++) {
                    for (EnemyDescriptor d : mWave.getEnemies()) {
                        if (MathUtils.equals(d.getDelay(), 0f, 0.1f)) {
                            offsetX += d.getOffsetX();
                            offsetY += d.getOffsetY();
                        } else {
                            offsetX = d.getOffsetX();
                            offsetY = d.getOffsetY();
                        }

                        final Enemy e = d.createInstance();
                        e.addListener(mObjectListener);
                        e.modifyHealth(mHealthModifier);
                        e.modifyReward(mRewardModifier);
                        e.setPath(mManager.getLevel().getPaths().get(d.getPathIndex()));
                        e.move(offsetX, offsetY);

                        if (i > 0 || mWave.getEnemies().indexOf(d) > 0) {
                            delay += (int)d.getDelay();
                        }

                        final int thisDelay = delay;
                        mEarlyBonus += e.getReward();

                        mGame.add(new Runnable() {
                            TickTimer mTimer = TickTimer.createInterval(thisDelay);

                            @Override
                            public void run() {
                                if (mTimer.tick()) {
                                    mGame.add(e);
                                    mGame.remove(this);
                                }
                            }
                        });
                    }
                }

                onStarted();
                mGame.remove(this);
            }
        });
    }

    public void abort() {
        mGame.add(new Runnable() {
            @Override
            public void run() {
                mAborted = true;
                onAborted();

                mGame.remove(this);
            }
        });
    }

    public int getReward() {
        return mWaveReward;
    }

    public void giveReward() {
        mGame.add(new Runnable() {
            @Override
            public void run() {
                mManager.giveCredits(mWaveReward, true);
                mWaveReward = 0;
                mGame.remove(this);
            }
        });
    }

    /*
    ------ Listener Stuff ------
     */

    public void addListener(Listener listener) {
        mListeners.add(listener);
    }

    public void removeListener(Listener listener) {
        mListeners.remove(listener);
    }


    private void onStarted() {
        for (Listener l : mListeners) {
            l.onStarted(this);
        }
    }

    private void onAborted() {
        for (Listener l : mListeners) {
            l.onAborted(this);
        }
    }

    private void onFinished() {
        for (Listener l : mListeners) {
            l.onFinished(this);
        }
    }

    private void onEnemyAdded(Enemy e) {
        for (Listener l : mListeners) {
            l.onEnemyAdded(this, e);
        }
    }

    private void onEnemyRemoved(Enemy e) {
        for (Listener l : mListeners) {
            l.onEnemyRemoved(this, e);
        }
    }
}
