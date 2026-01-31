package com.janilla.net;

import java.time.Instant;

public record ThreadAndInstant(Thread thread, Instant instant) {
}
