import xml.etree.ElementTree as ET
import re

file_path = "app/src/main/res/layout/layout_eld_graph.xml"

with open(file_path, "r") as f:
    text = f.read()

# We need to manually insert x_axis_labels at the top of graph_container and remove the old X-Axis Labels block.
# Let's find:
#         <!-- Y-Axis Labels -->
#         <LinearLayout
# and insert our new block BEFORE it.

labels = ["M", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "N", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11"]
tvs = "\n".join([f'                <TextView android:layout_width="0dp" android:layout_height="match_parent" android:layout_weight="1" android:text="{l}" android:textSize="7sp" android:textColor="@color/home_text_sub" android:gravity="start|bottom" android:singleLine="true"/>' for l in labels])

x_axis_xml = f"""
        <!-- X-Axis Labels Container -->
        <FrameLayout
            android:id="@+id/x_axis_labels"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            app:layout_constraintTop_toTopOf="parent"
            app:layout_constraintEnd_toEndOf="@id/graph"
            app:layout_constraintStart_toStartOf="@id/graph">
            
            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:weightSum="24"
                android:orientation="horizontal">
{tvs}
            </LinearLayout>

            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_gravity="end"
                android:text="M"
                android:textSize="7sp"
                android:textColor="@color/home_text_sub"
                android:gravity="end|bottom" />
        </FrameLayout>
"""

# Find where to insert
insert_pos = text.find("        <!-- Y-Axis Labels -->\n        <LinearLayout\n            android:id=\"@+id/y_axis\"")
if insert_pos != -1:
    text = text[:insert_pos] + x_axis_xml + "\n" + text[insert_pos:]

# Remove the old X-Axis Labels block at the bottom
old_x_axis_start = text.find("        <!-- X-Axis Labels -->\n        <LinearLayout\n            android:layout_width=\"0dp\"")
old_x_axis_end = text.find("        </LinearLayout>", old_x_axis_start) + len("        </LinearLayout>")

if old_x_axis_start != -1 and old_x_axis_end != -1:
    text = text[:old_x_axis_start] + text[old_x_axis_end:]

# Now replace layout_constraintTop_toBottomOf="@id/x_axis_labels"  on y_axis, etc., if needed.
# Since x_axis_labels is inside graph_container, we want ALL interior elements (except x_axis_labels) to be constrained below x_axis_labels
# Wait, if x_axis_labels is at the top of graph_container with constraintTop_toTopOf="parent", the rest just need to be bottom_toTopOf parent?
# Currently y_axis has: app:layout_constraintTop_toBottomOf="@id/x_axis_labels"
# hours_column has: app:layout_constraintTop_toBottomOf="@id/x_axis_labels"
# graph has: app:layout_constraintTop_toBottomOf="@id/x_axis_labels"
# That is ALREADY set by our previous script.

# Just also remove marginBottom="17dp" from graph, hours_column, y_axis if they are still there
text = text.replace('android:layout_marginBottom="17dp"', '')

with open(file_path, "w") as f:
    f.write(text)

print("Fixed layout missing block!")
