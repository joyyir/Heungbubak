package pe.joyyir.Heungbubak.Common.Util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
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

    public List<?> convertSheetToObjectList(List<List<String>> sheet, Class clazz) {
        List<Object> list = new ArrayList<>();

        try {
            List<String> column = sheet.get(0);
            for (int i = 1; i < sheet.size(); i++) {
                List<String> row = sheet.get(i);
                Object obj = clazz.newInstance();
                for (int j = 0; j < column.size(); j++) {
                    String fieldName = column.get(j);
                    Field field = clazz.getDeclaredField(fieldName);
                    Method method = clazz.getMethod("set" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1), field.getType());
                    method.invoke(obj, row.get(j));
                }
                list.add(obj);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }
}
