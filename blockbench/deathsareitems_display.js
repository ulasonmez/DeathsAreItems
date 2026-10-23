/* Display editor for the exact meshes captured by tools/blockbench/export.init.gradle. */
(function () {
    let format, itemProperty, originalUpdate, onLoad, onMode;
    let lastProject, lastSlot;
    const formatId = 'deathsareitems_display';

    function showContext() {
        if (!Project || Format.id !== formatId) return;
        const slot = Modes.display ? DisplayMode.display_slot : 'gui';
        if (lastProject === Project && lastSlot === slot) return;
        lastProject = Project;
        lastSlot = slot;
        Mesh.all.forEach(mesh => {
            mesh.visibility = mesh.name.startsWith(slot + '|');
            mesh.preview_controller.updateVisibility(mesh);
        });
    }

    Plugin.register('deathsareitems_display', {
        title: 'Deaths Are Items — Duruş Düzenleyici',
        author: 'DeathsAreItems',
        description: 'Orijinal modellerin görünümünü koruyarak el, envanter ve yer duruşlarını düzenler. Ctrl/Cmd+S ile .bbmodel kaydedin.',
        icon: 'tune',
        version: '1.0.0',
        min_version: '4.12.0',
        variant: 'both',
        contributes: {formats: [formatId]},
        onload() {
            itemProperty = new Property(ModelProject, 'string', 'dai_item');
            format = new ModelFormat(formatId, {
                name: 'Deaths Are Items — Display',
                description: 'Oyundan alınan model önizlemeleri; yalnızca display ayarları moda geri aktarılır.',
                icon: 'tune',
                category: 'minecraft',
                meshes: true,
                edit_mode: true,
                paint_mode: false,
                display_mode: true,
                centered_grid: false,
                per_texture_uv_size: true,
                single_texture: false,
                render_sides: true,
                rotate_cubes: true,
                model_identifier: false
            });
            originalUpdate = DisplayMode.updateDisplayBase;
            DisplayMode.updateDisplayBase = function (...args) {
                const result = originalUpdate.apply(this, args);
                showContext();
                return result;
            };
            onLoad = () => {
                lastProject = null;
                showContext();
            };
            onMode = () => { lastSlot = null; showContext(); };
            Codecs.project.on('parsed', onLoad);
            Blockbench.on('select_project', onLoad);
            Blockbench.on('select_mode', onMode);
            showContext();
        },
        onunload() {
            DisplayMode.updateDisplayBase = originalUpdate;
            Codecs.project.removeListener('parsed', onLoad);
            Blockbench.removeListener('select_project', onLoad);
            Blockbench.removeListener('select_mode', onMode);
            format.delete();
            itemProperty.delete();
        }
    });
})();
