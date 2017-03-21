package lee.study.bean;

/**
 * Created by yijia on 2017/3/17.
 */
public class Hero {
    private int id;
    //行走速度
    private int speed = 1;
    //方向
    private int status = 0;
    //坐标
    private int x = 32;
    private int y = 64;

    public Hero(){
        id = Room.buildUserId();
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getSpeed() {
        return speed;
    }

    public void setSpeed(int speed) {
        this.speed = speed;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }
}
