package ke.co.talin.myapplication.Model;

public class Rating {
    private String userPhone; //both key and value
    private String foodId;
    private String ratingValue;
    private String comments;

    public Rating() {
    }

    public Rating(String userPhone, String comments, String ratingValue, String foodId) {
        this.userPhone = userPhone;
        this.comments = comments;
        this.ratingValue = ratingValue;
        this.foodId = foodId;
    }

    public String getUserPhone() {
        return userPhone;
    }

    public void setUserPhone(String userPhone) {
        this.userPhone = userPhone;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public String getRatingValue() {
        return ratingValue;
    }

    public void setRatingValue(String ratingValue) {
        this.ratingValue = ratingValue;
    }

    public String getFoodId() {
        return foodId;
    }

    public void setFoodId(String foodId) {
        this.foodId = foodId;
    }
}
