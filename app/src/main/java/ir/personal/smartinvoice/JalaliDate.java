package ir.personal.smartinvoice;

import java.util.Calendar;
import java.util.Locale;

final class JalaliDate {

    private JalaliDate() {
        // Utility class
    }

    static String format(Calendar date, String separator) {
        int[] jalali = gregorianToJalali(
                date.get(Calendar.YEAR),
                date.get(Calendar.MONTH) + 1,
                date.get(Calendar.DAY_OF_MONTH)
        );

        return String.format(
                Locale.US,
                "%04d%s%02d%s%02d",
                jalali[0],
                separator,
                jalali[1],
                separator,
                jalali[2]
        );
    }

    private static int[] gregorianToJalali(int gy, int gm, int gd) {
        int[] monthDays = {
                0, 31, 59, 90, 120, 151,
                181, 212, 243, 273, 304, 334
        };

        int jy;

        if (gy > 1600) {
            jy = 979;
            gy -= 1600;
        } else {
            jy = 0;
            gy -= 621;
        }

        int adjustedYear = (gm > 2) ? gy + 1 : gy;

        int days = 365 * gy
                + (adjustedYear + 3) / 4
                - (adjustedYear + 99) / 100
                + (adjustedYear + 399) / 400
                - 80
                + gd
                + monthDays[gm - 1];

        jy += 33 * (days / 12053);
        days %= 12053;

        jy += 4 * (days / 1461);
        days %= 1461;

        if (days > 365) {
            jy += (days - 1) / 365;
            days = (days - 1) % 365;
        }

        int jm;
        int jd;

        if (days < 186) {
            jm = 1 + days / 31;
            jd = 1 + days % 31;
        } else {
            jm = 7 + (days - 186) / 30;
            jd = 1 + (days - 186) % 30;
        }

        return new int[]{jy, jm, jd};
    }
          }
