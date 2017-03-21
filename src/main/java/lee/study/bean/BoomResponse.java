package lee.study.bean;

/**
 * Created by yijia on 2017/3/17.
 */
public class BoomResponse {
    //0分配英雄ID 1创建英雄
    private int status;
    private Object bussObj;

    public BoomResponse() {
    }

    public BoomResponse(int status, Object bussObj) {
        this.status = status;
        this.bussObj = bussObj;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public Object getBussObj() {
        return bussObj;
    }

    public void setBussObj(Object bussObj) {
        this.bussObj = bussObj;
    }
}
