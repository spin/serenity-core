package net.thucydides.core.steps;

import net.thucydides.core.annotations.Fields;
import net.thucydides.core.annotations.InvalidStepsFieldException;
import net.thucydides.core.annotations.Steps;
import net.thucydides.core.reflection.FieldSetter;
import net.thucydides.core.util.Inflector;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * Used to identify Step library fields that need to be instantiated.
 * 
 * @author johnsmart
 * 
 */
public class StepsAnnotatedField {

    private Field field;
    
    private static final String NO_ANNOTATED_FIELD_ERROR
        = "No field annotated with @Steps was found in the test case.";

    public String getFieldName() {
        return field.getName();
    }

    /**
     * Find the first field in the class annotated with the <b>Managed</b> annotation.
     */
    public static List<StepsAnnotatedField> findMandatoryAnnotatedFields(final Class<?> clazz) {

        List<StepsAnnotatedField> annotatedFields = findOptionalAnnotatedFields(clazz);
        if (annotatedFields.isEmpty()) {
            throw new InvalidStepsFieldException(NO_ANNOTATED_FIELD_ERROR);
        }
        return annotatedFields;
    }

    /**
     * Find the fields in the class annotated with the <b>Step</b> annotation.
     */
    public static List<StepsAnnotatedField> findOptionalAnnotatedFields(final Class<?> clazz) {

        List<StepsAnnotatedField> annotatedFields = new ArrayList<StepsAnnotatedField>();
        for (Field field : Fields.of(clazz).allFields()) {
            if (fieldIsAnnotated(field)) {
                annotatedFields.add( new StepsAnnotatedField(field));
            }
        }
        return annotatedFields;
    }

    private static boolean fieldIsAnnotated(final Field aField) {
        Steps fieldAnnotation = annotationFrom(aField);
        return (fieldAnnotation != null);
    }

    private static Steps annotationFrom(final Field aField) {
        Steps annotationOnField = null;
        if (isFieldAnnotated(aField)) {
            annotationOnField = aField.getAnnotation(Steps.class);
        }
        return annotationOnField;
    }

    private static boolean isFieldAnnotated(final Field field) {
        return (fieldIsAnnotatedCorrectly(field));
    }

    private static boolean fieldIsAnnotatedCorrectly(final Field field) {
        return (field.getAnnotation(Steps.class) != null);
    }

    protected StepsAnnotatedField(final Field field) {
        this.field = field;
    }

    protected FieldSetter set(Object targetObject) {
        return new FieldSetter(field, targetObject);
    }

    public void setValue(final Object field, final Object value) {
        try {
            set(field).to(value);
        } catch (IllegalAccessException e) {
            throw new InvalidStepsFieldException("Could not access or set field: " + field, e);
        }
    }

    public boolean isInstantiated(final Object testCase) {
        try {
            field.setAccessible(true);
            Object fieldValue = field.get(testCase);
            return (fieldValue != null);
        } catch (IllegalAccessException e) {
            throw new InvalidStepsFieldException("Could not access or set @Steps field: " + field, e);
        }
    }

    public Class<?> getFieldClass() {
        return field.getType();
    }

    public boolean isSharedInstance() {
        return field.getAnnotation(Steps.class).shared();
    }

    public boolean isUniqueInstance() {
        return field.getAnnotation(Steps.class).uniqueInstance();
    }

    public Optional<String> actor() {
        String nameValue = field.getAnnotation(Steps.class).actor();
        if (isBlank(nameValue)) {
            return Optional.empty();
        }
        return Optional.of(nameValue);
    }

    public void assignActorNameIn(Object steps) {

        String actorName = actor().orElse(humanReadable(getFieldName()));

        if (isNotBlank(actorName)) {
            actorFieldIn(steps).ifPresent(
                    (Field field) -> {
                        assignValueToField(field, steps, actorName);
                    }
            );
        }
    }

    private String humanReadable(String fieldName) {
        return new Inflector().of(fieldName).asATitle().toString();
    }

    private void assignValueToField(Field field, Object steps, String value) {
        field.setAccessible(true);
        try {
            field.set(steps, value);
        } catch (IllegalAccessException e) {
            throw new InvalidStepsFieldException("Could not access or set name field: " + field, e);
        }
    }

    private Optional<Field> actorFieldIn(Object steps) {
        return Arrays.stream(steps.getClass().getSuperclass().getDeclaredFields())
                .filter(field -> field.getName().equals("actor")
                        && field.getType().equals(String.class))
                .findFirst();
    }

}
