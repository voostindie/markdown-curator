package nl.ulso.markdown_curator;

record ChangeImpl<T>(T object, Class<T> objectType, Change.Kind kind)
    implements Change<T>
{
}
