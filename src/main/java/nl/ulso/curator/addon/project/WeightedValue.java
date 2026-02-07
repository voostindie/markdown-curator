package nl.ulso.curator.addon.project;

/// The value/weight part of an [AttributeValue], for use in internal data structures that are kept
/// in memory for a long time.
///
/// In a weighted value, it is the _weight_ that matters, not the value itself: when comparing or
/// ordering weighted values, only the weights are considered.
///
/// @see DefaultAttributeRegistry
record WeightedValue(Object value, int weight)
    implements Comparable<WeightedValue>
{
    @Override
    public boolean equals(Object object)
    {
        if (object == null || getClass() != object.getClass())
        {
            return false;
        }
        var other = (WeightedValue) object;
        return this.weight == other.weight;
    }

    @Override
    public int hashCode()
    {
        return Integer.hashCode(weight);
    }

    @Override
    public int compareTo(WeightedValue other)
    {
        return Integer.compare(weight, other.weight);
    }
}
