package portal.expenses.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordEncoderUtil {

    private static final Logger logger = LoggerFactory.getLogger(PasswordEncoderUtil.class);

    // This class is intended to provide utility for password encoding.
    // The main method for generating password hashes for initial users has been removed
    // to prevent hardcoding of sensitive information.
    // For generating password hashes, use a dedicated test or a separate utility process.
}
