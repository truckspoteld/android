import re

file_path = "app/src/main/res/layout/layout_eld_graph.xml"
with open(file_path, "r") as f:
    content = f.read()

# Generate the 24 textviews
labels = ["M", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "N", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11"]
tvs = "\n".join([f'                <TextView android:layout_width="0dp" android:layout_height="match_parent" android:layout_weight="1" android:text="{l}" android:textSize="10sp" android:textColor="@color/home_text_sub" android:gravity="start|center_vertical" android:singleLine="true"/>' for l in labels])

x_axis_xml = f"""
        <!-- X-Axis Labels Container -->
        <FrameLayout
            android:id="@+id/x_axis_labels"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_marginBottom="4dp"
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
                android:textSize="10sp"
                android:textColor="@color/home_text_sub"
                android:gravity="end|center_vertical" />
        </FrameLayout>
"""

# Replace y_axis margins and constraints
content = re.sub(
    r'android:id="@+id/y_axis"([^>]*?)android:layout_marginBottom="17dp"',
    r'android:id="@+id/y_axis"\1',
    content, flags=re.DOTALL
)

content = re.sub(
    r'android:id="@+id/hours_column"([^>]*?)android:layout_marginBottom="17dp"',
    r'android:id="@+id/hours_column"\1',
    content, flags=re.DOTALL
)

content = re.sub(
    r'android:id="@+id/graph"([^>]*?)android:layout_marginBottom="17dp"',
    r'android:id="@+id/graph"\1',
    content, flags=re.DOTALL
)

# Change top constraint for the inner components
content = content.replace('app:layout_constraintTop_toTopOf="parent"', 'app:layout_constraintTop_toBottomOf="@id/x_axis_labels"')

# But we need to keep constraintTop_toTopOf="parent" for the legend, etc?
# Wait, the inner components are inside `<androidx.constraintlayout.widget.ConstraintLayout android:id="@+id/graph_container"`
# Let's cleanly replace the inside of graph_container.

container_match = re.search(r'<androidx.constraintlayout.widget.ConstraintLayout\s+android:id="@+id/graph_container".*?>\s*(.*?)\s*</androidx.constraintlayout.widget.ConstraintLayout>', content, flags=re.DOTALL)

if container_match:
    inner_xml = container_match.group(1)
    
    # Remove the old X-Axis Labels block
    inner_xml = re.sub(r'<!-- X-Axis Labels -->.*?</LinearLayout>', '', inner_xml, flags=re.DOTALL)
    
    # Modify y_axis, hours_column, graph to remove marginBottom
    inner_xml = inner_xml.replace('android:layout_marginBottom="17dp"', '')
    # Change top constraint to refer to x_axis_labels
    inner_xml = inner_xml.replace('app:layout_constraintTop_toTopOf="parent"', 'app:layout_constraintTop_toBottomOf="@id/x_axis_labels"')
    
    # Add new x_axis_labels at the top
    new_inner_xml = x_axis_xml + "\n" + inner_xml
    
    content = content.replace(container_match.group(1), new_inner_xml)

with open(file_path, "w") as f:
    f.write(content)

print("Updated layout!")

