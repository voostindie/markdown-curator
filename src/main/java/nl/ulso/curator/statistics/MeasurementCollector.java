package nl.ulso.curator.statistics;

/// Collects measurements for entities within modules, for now only totals.
///
/// Measurements apply to entities in modules. Typically, an entity is a Java class, and a
/// module is the package name of the class. But in the end they're just [String]s, so
/// implementations can measure other things as well.
public interface MeasurementCollector
{
    /// Sets the total for a specific entity class. The name of the entity is the simple name of the
    /// class in lower case, and the name of the module is the last part of the package name of the
    /// class.
    MeasurementCollector total(Class<?> entityClass, long count);

    /// Sets the total for a specific entity within a module.
    MeasurementCollector total(String module, String entity, long count);

    /// Sets the model to collect statistics for.
    MeasurementCollector forModule(String module);

    /// Sets the total for a specific entity within the current module.
    ///
    /// @throws IllegalStateException if a module has not been set.
    /// @see [#forModule(String)
    MeasurementCollector total(String entity, long count);
}
