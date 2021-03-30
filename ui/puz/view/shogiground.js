"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.makeConfig = void 0;
const coordsColor_1 = require("common/coordsColor");
const resize_1 = require("common/resize");
function makeConfig(opts, pref, userMove) {
    return {
        fen: opts.fen,
        orientation: opts.orientation,
        turnColor: opts.turnColor,
        check: opts.check,
        lastMove: opts.lastMove,
        coordinates: pref.coords !== 0,
        addPieceZIndex: pref.is3d,
        movable: {
            free: false,
            color: opts.movable.color,
            dests: opts.movable.dests,
            showDests: pref.destination,
            rookCastle: pref.rookCastle,
        },
        draggable: {
            enabled: pref.moveEvent > 0,
            showGhost: pref.highlight,
        },
        selectable: {
            enabled: pref.moveEvent !== 1,
        },
        events: {
            move: userMove,
            insert(elements) {
                resize_1.default(elements, 1, 0, p => p == 0);
                if (pref.coords == 1)
                    coordsColor_1.default();
            },
        },
        premovable: {
            enabled: false,
        },
        drawable: {
            enabled: true,
        },
        highlight: {
            lastMove: pref.highlight,
            check: pref.highlight,
        },
        animation: {
            enabled: false,
        },
        disableContextMenu: true,
    };
}
exports.makeConfig = makeConfig;
//# sourceMappingURL=data:application/json;base64,eyJ2ZXJzaW9uIjozLCJmaWxlIjoic2hvZ2lncm91bmQuanMiLCJzb3VyY2VSb290IjoiIiwic291cmNlcyI6WyIuLi9zcmMvdmlldy9zaG9naWdyb3VuZC50cyJdLCJuYW1lcyI6W10sIm1hcHBpbmdzIjoiOzs7QUFBQSxvREFBbUQ7QUFDbkQsMENBQXlDO0FBSXpDLFNBQWdCLFVBQVUsQ0FBQyxJQUFjLEVBQUUsSUFBYyxFQUFFLFFBQWtCO0lBQzNFLE9BQU87UUFDTCxHQUFHLEVBQUUsSUFBSSxDQUFDLEdBQUc7UUFDYixXQUFXLEVBQUUsSUFBSSxDQUFDLFdBQVc7UUFDN0IsU0FBUyxFQUFFLElBQUksQ0FBQyxTQUFTO1FBQ3pCLEtBQUssRUFBRSxJQUFJLENBQUMsS0FBSztRQUNqQixRQUFRLEVBQUUsSUFBSSxDQUFDLFFBQVE7UUFDdkIsV0FBVyxFQUFFLElBQUksQ0FBQyxNQUFNLEtBQUssQ0FBQztRQUM5QixjQUFjLEVBQUUsSUFBSSxDQUFDLElBQUk7UUFDekIsT0FBTyxFQUFFO1lBQ1AsSUFBSSxFQUFFLEtBQUs7WUFDWCxLQUFLLEVBQUUsSUFBSSxDQUFDLE9BQVEsQ0FBQyxLQUFLO1lBQzFCLEtBQUssRUFBRSxJQUFJLENBQUMsT0FBUSxDQUFDLEtBQUs7WUFDMUIsU0FBUyxFQUFFLElBQUksQ0FBQyxXQUFXO1lBQzNCLFVBQVUsRUFBRSxJQUFJLENBQUMsVUFBVTtTQUM1QjtRQUNELFNBQVMsRUFBRTtZQUNULE9BQU8sRUFBRSxJQUFJLENBQUMsU0FBUyxHQUFHLENBQUM7WUFDM0IsU0FBUyxFQUFFLElBQUksQ0FBQyxTQUFTO1NBQzFCO1FBQ0QsVUFBVSxFQUFFO1lBQ1YsT0FBTyxFQUFFLElBQUksQ0FBQyxTQUFTLEtBQUssQ0FBQztTQUM5QjtRQUNELE1BQU0sRUFBRTtZQUNOLElBQUksRUFBRSxRQUFRO1lBQ2QsTUFBTSxDQUFDLFFBQVE7Z0JBQ2IsZ0JBQVksQ0FBQyxRQUFRLEVBQUUsQ0FBQyxFQUFFLENBQUMsRUFBRSxDQUFDLENBQUMsRUFBRSxDQUFDLENBQUMsSUFBSSxDQUFDLENBQUMsQ0FBQztnQkFDMUMsSUFBSSxJQUFJLENBQUMsTUFBTSxJQUFJLENBQUM7b0JBQUUscUJBQWlCLEVBQUUsQ0FBQztZQUM1QyxDQUFDO1NBQ0Y7UUFDRCxVQUFVLEVBQUU7WUFDVixPQUFPLEVBQUUsS0FBSztTQUNmO1FBQ0QsUUFBUSxFQUFFO1lBQ1IsT0FBTyxFQUFFLElBQUk7U0FDZDtRQUNELFNBQVMsRUFBRTtZQUNULFFBQVEsRUFBRSxJQUFJLENBQUMsU0FBUztZQUN4QixLQUFLLEVBQUUsSUFBSSxDQUFDLFNBQVM7U0FDdEI7UUFDRCxTQUFTLEVBQUU7WUFDVCxPQUFPLEVBQUUsS0FBSztTQUNmO1FBQ0Qsa0JBQWtCLEVBQUUsSUFBSTtLQUN6QixDQUFDO0FBQ0osQ0FBQztBQTdDRCxnQ0E2Q0MiLCJzb3VyY2VzQ29udGVudCI6WyJpbXBvcnQgY2hhbmdlQ29sb3JIYW5kbGUgZnJvbSAnY29tbW9uL2Nvb3Jkc0NvbG9yJztcbmltcG9ydCByZXNpemVIYW5kbGUgZnJvbSAnY29tbW9uL3Jlc2l6ZSc7XG5pbXBvcnQgeyBDb25maWcgYXMgQ2dDb25maWcgfSBmcm9tICdzaG9naWdyb3VuZC9jb25maWcnO1xuaW1wb3J0IHsgUHV6UHJlZnMsIFVzZXJNb3ZlIH0gZnJvbSAnLi4vaW50ZXJmYWNlcyc7XG5cbmV4cG9ydCBmdW5jdGlvbiBtYWtlQ29uZmlnKG9wdHM6IENnQ29uZmlnLCBwcmVmOiBQdXpQcmVmcywgdXNlck1vdmU6IFVzZXJNb3ZlKTogQ2dDb25maWcge1xuICByZXR1cm4ge1xuICAgIGZlbjogb3B0cy5mZW4sXG4gICAgb3JpZW50YXRpb246IG9wdHMub3JpZW50YXRpb24sXG4gICAgdHVybkNvbG9yOiBvcHRzLnR1cm5Db2xvcixcbiAgICBjaGVjazogb3B0cy5jaGVjayxcbiAgICBsYXN0TW92ZTogb3B0cy5sYXN0TW92ZSxcbiAgICBjb29yZGluYXRlczogcHJlZi5jb29yZHMgIT09IDAsXG4gICAgYWRkUGllY2VaSW5kZXg6IHByZWYuaXMzZCxcbiAgICBtb3ZhYmxlOiB7XG4gICAgICBmcmVlOiBmYWxzZSxcbiAgICAgIGNvbG9yOiBvcHRzLm1vdmFibGUhLmNvbG9yLFxuICAgICAgZGVzdHM6IG9wdHMubW92YWJsZSEuZGVzdHMsXG4gICAgICBzaG93RGVzdHM6IHByZWYuZGVzdGluYXRpb24sXG4gICAgICByb29rQ2FzdGxlOiBwcmVmLnJvb2tDYXN0bGUsXG4gICAgfSxcbiAgICBkcmFnZ2FibGU6IHtcbiAgICAgIGVuYWJsZWQ6IHByZWYubW92ZUV2ZW50ID4gMCxcbiAgICAgIHNob3dHaG9zdDogcHJlZi5oaWdobGlnaHQsXG4gICAgfSxcbiAgICBzZWxlY3RhYmxlOiB7XG4gICAgICBlbmFibGVkOiBwcmVmLm1vdmVFdmVudCAhPT0gMSxcbiAgICB9LFxuICAgIGV2ZW50czoge1xuICAgICAgbW92ZTogdXNlck1vdmUsXG4gICAgICBpbnNlcnQoZWxlbWVudHMpIHtcbiAgICAgICAgcmVzaXplSGFuZGxlKGVsZW1lbnRzLCAxLCAwLCBwID0+IHAgPT0gMCk7XG4gICAgICAgIGlmIChwcmVmLmNvb3JkcyA9PSAxKSBjaGFuZ2VDb2xvckhhbmRsZSgpO1xuICAgICAgfSxcbiAgICB9LFxuICAgIHByZW1vdmFibGU6IHtcbiAgICAgIGVuYWJsZWQ6IGZhbHNlLFxuICAgIH0sXG4gICAgZHJhd2FibGU6IHtcbiAgICAgIGVuYWJsZWQ6IHRydWUsXG4gICAgfSxcbiAgICBoaWdobGlnaHQ6IHtcbiAgICAgIGxhc3RNb3ZlOiBwcmVmLmhpZ2hsaWdodCxcbiAgICAgIGNoZWNrOiBwcmVmLmhpZ2hsaWdodCxcbiAgICB9LFxuICAgIGFuaW1hdGlvbjoge1xuICAgICAgZW5hYmxlZDogZmFsc2UsXG4gICAgfSxcbiAgICBkaXNhYmxlQ29udGV4dE1lbnU6IHRydWUsXG4gIH07XG59XG4iXX0=