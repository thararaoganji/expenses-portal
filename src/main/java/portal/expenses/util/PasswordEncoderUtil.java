package portal.expenses.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordEncoderUtil {

    private static final Logger logger = LoggerFactory.getLogger(PasswordEncoderUtil.class);

    private PasswordEncoderUtil() {
        // Utility class
    }

    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        if (args.length > 0) {
            for (String rawPassword : args) {
                if (logger.isInfoEnabled()) {
                    logger.info("Encoded Password: {}", encoder.encode(rawPassword));
                }
            }
        } else {
            if (logger.isInfoEnabled()) {
                logger.info("Usage: java PasswordEncoderUtil <password1> <password2> ...");
            }
        }
    }
}
