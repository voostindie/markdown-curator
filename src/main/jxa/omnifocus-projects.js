function run(argv) {
    let folderName = argv[0];
    let projects = Application('OmniFocus')
        .defaultDocument()
        .folders
        .byName(folderName)
        .flattenedProjects
        .whose({
            _match: [ObjectSpecifier().status, 'active']
        });
    let ids = projects.id();
    let names = projects.name();
    var i = 0;
    let items = ids.map(function (id) {
        return {
            id: id,
            name: names[i++]
        };
    });
    return JSON.stringify(items);
}