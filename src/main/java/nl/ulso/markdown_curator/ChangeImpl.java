package nl.ulso.markdown_curator;

record ChangeImpl<T>(T object, Class<T> objectType, Change.Kind kind)
    implements Change<T>
{
    @SuppressWarnings("unchecked")
    @Override
    public <U> Change<U> as(Class<U> objectType)
    {
        if (!this.objectType.equals(objectType))
        {
            throw new IllegalStateException("Cannot cast change to different object type");
        }
        return (Change<U>) this;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <U> U objectAs(Class<U> objectType)
    {
        if (!this.objectType.equals(objectType))
        {
            throw new IllegalStateException("Cannot cast change to different object type");
        }
        return (U) object;
    }
}
