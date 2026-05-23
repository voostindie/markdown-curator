function run(argv) {
    const folderName = argv[0];
    const projects = Application('OmniFocus')
        .defaultDocument()
        .folders
        .byName(folderName)
        .flattenedProjects;
    const ids = projects.id();
    const names = projects.name();
    const statuses = projects.status();
    let i = 0;
    const items = ids.map(function (id) {
        return {
            id: id,
            name: names[i],
            status: statuses[i++],
        };
    });
    return JSON.stringify(items);
}