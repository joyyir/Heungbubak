package pe.joyyir.Heungbubak.Common.Util;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CsvUtil {
    public List<List<String>> convertObjectListToSheet(List<?> changeList) {
        List<List<String>> sheet = new ArrayList<>();

        try {
            Class clazz = changeList.get(0).getClass();

            sheet = new ArrayList<>();
            List<String> column = (Arrays.asList(clazz.getDeclaredFields()).stream().map(x -> x.getName()).collect(Collectors.toList()));
            sheet.add(column);
            if (CmnUtil.isNotEmpty(changeList)) {
                for (Object vo : changeList) {
                    List<String> row = new ArrayList<>();
                    for (int i = 0; i < column.size(); i++) {
                        Field field = clazz.getDeclaredField(column.get(i));
                        field.setAccessible(true);
                        Object value = field.get(vo);
                        row.add(value.toString());
                    }
                    sheet.add(row);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return sheet;
    }
}
