package org.example.service.enums;

public enum ServiceCommand {
    HELP("задать вопрос"),
    ADMIN("/administrator"),
    CANCEL("/cancel"),
    START("/start"),
    SET_NAME("участвую"),
    FEEDBACK("оставить отзыв"),
    ADMIN_PASS("123654987l"),
    GETUSER("/getcontact"),
    EXIT_FROM_ADMIN("/exit"),
    sendReminders("/sendReminders"),
    sendRemindersWithText("рассылка")
    ;
    private final String value;

    ServiceCommand(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
	return value;
    }

    public static ServiceCommand fromValue(String v) {
        for (ServiceCommand c: ServiceCommand.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        return null;
    }
}
