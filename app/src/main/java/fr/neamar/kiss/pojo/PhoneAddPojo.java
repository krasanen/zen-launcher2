package fr.neamar.kiss.pojo;

public class PhoneAddPojo extends Pojo {
    public final String phone;

    public PhoneAddPojo(String id, String phone) {
        super(id);

        this.phone = phone;
    }
}