package lee.study.bean;

/**
 * Created by yijia on 2017/3/17.
 */
public class BoomRequest {
    private int status;
    private Hero bussObj = new Hero();

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public Hero getBussObj() {
        return bussObj;
    }

    public void setBussObj(Hero bussObj) {
        this.bussObj = bussObj;
    }
}
