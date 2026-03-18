import re

file_path = "app/src/main/res/layout/layout_eld_graph.xml"
with open(file_path, "r") as f:
    orig_content = f.read()

# Generate the 24 textviews
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

# reset content to original, perform all inner xml operations first
content = orig_content
container_match = re.search(r'(<androidx\.constraintlayout\.widget\.ConstraintLayout\s+android:id="@+id/graph_container".*?>)(.*?)(</androidx\.constraintlayout\.widget\.ConstraintLayout>)', content, flags=re.DOTALL)

if container_match:
    start_tag = container_match.group(1)
    inner_xml = container_match.group(2)
    end_tag = container_match.group(3)
    
    # Remove the old X-Axis Labels block
    inner_xml = re.sub(r'<!-- X-Axis Labels -->.*?layout_height="@dimen/_15sdp".*?LinearLayout>', '', inner_xml, flags=re.DOTALL)
    
    # Modify y_axis, hours_column, graph to remove marginBottom
    inner_xml = re.sub(r'android:layout_marginBottom="17dp"', '', inner_xml)
    
    # Change top constraint to refer to x_axis_labels
    inner_xml = inner_xml.replace('app:layout_constraintTop_toTopOf="parent"', 'app:layout_constraintTop_toBottomOf="@id/x_axis_labels"')
    inner_xml = inner_xml.replace('app:layout_constraintTop_toBottomOf="@id/x_axis_labels"', 'app:layout_constraintTop_toBottomOf="@id/x_axis_labels"') # no-op just in case
    
    # Add new x_axis_labels at the top
    new_inner_xml = "\n" + x_axis_xml + inner_xml
    
    content = content[:container_match.start()] + start_tag + new_inner_xml + end_tag + content[container_match.end():]

with open(file_path, "w") as f:
    f.write(content)

print("Updated layout correctly!")

